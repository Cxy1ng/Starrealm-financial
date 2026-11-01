/**
 * 星界 (Star Realm) 全局密码与表单校验工具
 * 规范：
 * 1. 强密码：8-20 位，必须同时包含大写字母、小写字母、数字和至少一个特殊符号
 * 2. 手机号：中国大陆规范手机号（11位，1开头，第二位3-9）
 * 3. 邮箱：标准规范电子邮件格式
 */

export const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~])[A-Za-z\d!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~]{8,20}$/;

export const PHONE_REGEX = /^1[3-9]\d{9}$/;

export const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

export function validatePasswordStrength(pwd) {
  if (!pwd) return { valid: false, message: '请输入密码' };
  if (pwd.length < 8 || pwd.length > 20) {
    return { valid: false, message: '密码长度必须为 8-20 位' };
  }
  if (!/[a-z]/.test(pwd)) {
    return { valid: false, message: '密码必须包含至少一个小写字母 (a-z)' };
  }
  if (!/[A-Z]/.test(pwd)) {
    return { valid: false, message: '密码必须包含至少一个大写字母 (A-Z)' };
  }
  if (!/\d/.test(pwd)) {
    return { valid: false, message: '密码必须包含至少一个数字 (0-9)' };
  }
  if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~]/.test(pwd)) {
    return { valid: false, message: '密码必须包含至少一个特殊符号（如 !@#$%^&* 等）' };
  }
  return { valid: true };
}

/**
 * Element Plus 表单校验规则函数：强密码校验
 */
export const validatePasswordRule = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入密码'));
  }
  const check = validatePasswordStrength(value);
  if (!check.valid) {
    return callback(new Error(check.message));
  }
  callback();
};

/**
 * Element Plus 表单校验规则函数：手机号格式校验
 */
export const validatePhoneRule = (rule, value, callback) => {
  if (!value) {
    return callback(); // 非必填时由 required 规则控制；若填写则校验格式
  }
  if (!PHONE_REGEX.test(value)) {
    return callback(new Error('请输入规范的11位手机号（以1开头的有效号段）'));
  }
  callback();
};

/**
 * Element Plus 表单校验规则函数：邮箱格式校验
 */
export const validateEmailRule = (rule, value, callback) => {
  if (!value) {
    return callback(); // 非必填时由 required 规则控制；若填写则校验格式
  }
  if (!EMAIL_REGEX.test(value)) {
    return callback(new Error('请输入有效的邮箱地址（格式如 user@domain.com）'));
  }
  callback();
};

/**
 * 前端生成符合规则的高强度临时密码
 */
export function generateStrongPassword(length = 14) {
  const lowers = 'abcdefghijklmnopqrstuvwxyz';
  const uppers = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const digits = '0123456789';
  const specials = '!@#$%^&*()_+-=';

  const randomPick = (chars) => chars[Math.floor(Math.random() * chars.length)];

  let pwd = [
    randomPick(lowers),
    randomPick(uppers),
    randomPick(digits),
    randomPick(specials)
  ];

  const all = lowers + uppers + digits + specials;
  for (let i = 4; i < length; i++) {
    pwd.push(randomPick(all));
  }

  // 洗牌
  for (let i = pwd.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [pwd[i], pwd[j]] = [pwd[j], pwd[i]];
  }

  return pwd.join('');
}
