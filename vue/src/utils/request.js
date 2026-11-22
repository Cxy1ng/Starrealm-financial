import axios from 'axios'
import router from "@/router/index.js";
import {ElMessage} from "element-plus";
import { getLoginSessionFromCookie, clearLoginSessionCookie } from "@/utils/cookie.js";

export const request = axios.create({
    // baseURL: process.env.VUE_APP_URL,
    baseURL: 'http://127.0.0.1:8080',
    timeout: 30000,
    withCredentials: true
})

request.interceptors.request.use(config => {
    if (config.data instanceof FormData) {
        delete config.headers['Content-Type'];
    } else if (!config.headers['Content-Type']) {
        config.headers['Content-Type'] = 'application/json; charset=UTF-8';
    }
    let user = JSON.parse(localStorage.getItem("loginToken") || '{}');
    if ((!user || !user.token) && typeof document !== 'undefined') {
        const cookieSession = getLoginSessionFromCookie();
        if (cookieSession && cookieSession.token) {
            user = cookieSession;
            localStorage.setItem("loginToken", JSON.stringify(cookieSession));
        }
    }
    if (user && user.token) {
        config.headers['token'] = user.token;
    }
    return config
}, error => {
    console.error('request error: ' + error)
    return Promise.reject(error)
});

request.interceptors.response.use(
    response => {
        let res = response.data;
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        if (res.code === 401) {
            const currentToken = JSON.parse(localStorage.getItem("loginToken") || '{}')?.token;
            const currentPath = router.currentRoute?.value?.path || window.location.pathname;
            localStorage.removeItem("loginToken");
            clearLoginSessionCookie();
            // 仅在用户原本处于登录状态且不在登录/注册页时提示（主动退出登录时不弹出“无效的token”）
            if (currentToken && currentPath !== '/login' && currentPath !== '/register') {
                ElMessage.error(res.msg);
            }
            if (currentPath !== '/login' && currentPath !== '/register') {
                router.replace('/login');
            }
        }
        return res;
    },
    error => {
        console.error('response error: ' + error)
        return Promise.reject(error)
    }
)

export default request
