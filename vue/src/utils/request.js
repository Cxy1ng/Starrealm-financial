import axios from 'axios'
import router from "@/router/index.js";
import {ElMessage} from "element-plus";

export const request = axios.create({
    // baseURL: process.env.VUE_APP_URL,
    baseURL: 'http://127.0.0.1:8080',
    timeout: 30000
})

request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json; charset=UTF-8';
    let user = JSON.parse(localStorage.getItem("loginToken") || '{}');
    config.headers['token'] = user.token
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
        if (res.code === 401 || res.code === 5012) {
            ElMessage.error(res.msg)
            if (res.code === 5012) {
                localStorage.removeItem('loginToken')
            }
            router.push('/login')
        }
        return res;
    },
    error => {
        console.error('response error: ' + error)
        return Promise.reject(error)
    }
)

export default request
