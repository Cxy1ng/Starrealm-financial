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
    return { valid: false, message: '密码长度必须在 8-20 位之间' };
  }
  if (!/(?=.*[a-z])/.test(pwd)) {
    return { valid: false, message: '密码必须包含至少一个小写字母 (a-z)' };
  }
  if (!/(?=.*[A-Z])/.test(pwd)) {
    return { valid: false, message: '密码必须包含至少一个大写字母 (A-Z)' };
  }
  if (!/(?=.*\d)/.test(pwd)) {
    return { valid: false, message: '密码必须包含至少一个数字 (0-9)' };
  }
  if (!/(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?`~])/.test(pwd)) {
    return { valid: false, message: '密码必须包含至少一个特殊符号 (如 @, #, $, ! 等)' };
  }
  return { valid: true };
}

export const passwordValidator = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入密码'));
  }
  const check = validatePasswordStrength(value);
  if (!check.valid) {
    return callback(new Error(check.message));
  }
  callback();
};

export const phoneValidator = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入手机号码'));
  }
  const trimmed = String(value).trim();
  if (!PHONE_REGEX.test(trimmed)) {
    return callback(new Error('请输入正确的11位手机号码（如13800138000）'));
  }
  callback();
};

export const optionalPhoneValidator = (rule, value, callback) => {
  if (!value) {
    return callback();
  }
  const trimmed = String(value).trim();
  if (!PHONE_REGEX.test(trimmed)) {
    return callback(new Error('请输入正确的11位手机号码'));
  }
  callback();
};

export const emailValidator = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('请输入电子邮箱'));
  }
  const trimmed = String(value).trim();
  if (!EMAIL_REGEX.test(trimmed)) {
    return callback(new Error('请输入合法的邮箱地址（如 example@domain.com）'));
  }
  callback();
};

export const optionalEmailValidator = (rule, value, callback) => {
  if (!value) {
    return callback();
  }
  const trimmed = String(value).trim();
  if (!EMAIL_REGEX.test(trimmed)) {
    return callback(new Error('请输入合法的邮箱地址'));
  }
  callback();
};

/**
 * 前端随机强密码生成器（同时满足8-20位、大小写字母、数字与特殊字符）
 */
export function generateStrongPassword(length = 14) {
  const lower = 'abcdefghijkmnpqrstuvwxyz';
  const upper = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
  const digits = '23456789';
  const symbols = '!@#$%^&*_-+=';

  const len = Math.max(10, Math.min(20, length));
  const chars = [
    lower[Math.floor(Math.random() * lower.length)],
    upper[Math.floor(Math.random() * upper.length)],
    digits[Math.floor(Math.random() * digits.length)],
    symbols[Math.floor(Math.random() * symbols.length)]
  ];

  const all = lower + upper + digits + symbols;
  while (chars.length < len) {
    chars.push(all[Math.floor(Math.random() * all.length)]);
  }

  // 洗牌打乱
  for (let i = chars.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [chars[i], chars[j]] = [chars[j], chars[i]];
  }

  return chars.join('');
}
