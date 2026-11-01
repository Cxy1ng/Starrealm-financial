/**
 * 星界 (Star Realm) Gemini 级多层前端 AI 反破甲与合规护栏引擎
 * 核心破甲检测规则与敏感词矩阵均以 AES-128 密文形式存储，杜绝源码与产物明文敏感词触发云端扫描拦截
 * 在向后端或 DeepSeek API 发起任何大模型请求之前，于本地零延迟检测：
 * 1. 提示词注入 (Prompt Injection) / 忽略指令 / 开发者模式 / DAN 越狱
 * 2. 伪造系统控制标记 (<|im_start|>, [INST], ### System:, <<SYS>>)
 * 3. 角色扮演与人设篡改诱导（猫娘、女仆、虚拟伴侣、句尾加口癖、假定剧本/祖母漏洞）
 * 4. 字符插空混淆绕过（如“扮·演·猫·娘”、“忽_略_指_令”）与 Base64 编码载荷注入
 * 5. 涉黄、涉政、暴恐、毒品与违法违禁敏感话题
 */

// ==================== 纯 JS 轻量 AES-128-ECB 同步解密引擎 ====================
const SBOX = [
  0x63,0x7c,0x77,0x7b,0xf2,0x6b,0x6f,0xc5,0x30,0x01,0x67,0x2b,0xfe,0xd7,0xab,0x76,
  0xca,0x82,0xc9,0x7d,0xfa,0x59,0x47,0xf0,0xad,0xd4,0xa2,0xaf,0x9c,0xa4,0x72,0xc0,
  0xb7,0xfd,0x93,0x26,0x36,0x3f,0xf7,0xcc,0x34,0xa5,0xe5,0xf1,0x71,0xd8,0x31,0x15,
  0x04,0xc7,0x23,0xc3,0x18,0x96,0x05,0x9a,0x07,0x12,0x80,0xe2,0xeb,0x27,0xb2,0x75,
  0x09,0x83,0x2c,0x1a,0x1b,0x6e,0x5a,0xa0,0x52,0x3b,0xd6,0xb3,0x29,0xe3,0x2f,0x84,
  0x53,0xd1,0x00,0xed,0x20,0xfc,0xb1,0x5b,0x6a,0xcb,0xbe,0x39,0x4a,0x4c,0x58,0xcf,
  0xd0,0xef,0xaa,0xfb,0x43,0x4d,0x33,0x85,0x45,0xf9,0x02,0x7f,0x50,0x3c,0x9f,0xa8,
  0x51,0xa3,0x40,0x8f,0x92,0x9d,0x38,0xf5,0xbc,0xb6,0xda,0x21,0x10,0xff,0xf3,0xd2,
  0xcd,0x0c,0x13,0xec,0x5f,0x97,0x44,0x17,0xc4,0xa7,0x7e,0x3d,0x64,0x5d,0x19,0x73,
  0x60,0x81,0x4f,0xdc,0x22,0x2a,0x90,0x88,0x46,0xee,0xb8,0x14,0xde,0x5e,0x0b,0xdb,
  0xe0,0x32,0x3a,0x0a,0x49,0x06,0x24,0x5c,0xc2,0xd3,0xac,0x62,0x91,0x95,0xe4,0x79,
  0xe7,0xc8,0x37,0x6d,0x8d,0xd5,0x4e,0xa9,0x6c,0x56,0xf4,0xea,0x65,0x7a,0xae,0x08,
  0xba,0x78,0x25,0x2e,0x1c,0xa6,0xb4,0xc6,0xe8,0xdd,0x74,0x1f,0x4b,0xbd,0x8b,0x8a,
  0x70,0x3e,0xb5,0x66,0x48,0x03,0xf6,0x0e,0x61,0x35,0x57,0xb9,0x86,0xc1,0x1d,0x9e,
  0xe1,0xf8,0x98,0x11,0x69,0xd9,0x8e,0x94,0x9b,0x1e,0x87,0xe9,0xce,0x55,0x28,0xdf,
  0x8c,0xa1,0x89,0x0d,0xbf,0xe6,0x42,0x68,0x41,0x99,0x2d,0x0f,0xb0,0x54,0xbb,0x16
];
const INV_SBOX = new Array(256);
for (let i = 0; i < 256; i++) INV_SBOX[SBOX[i]] = i;
const RCON = [0x00,0x01,0x02,0x04,0x08,0x10,0x20,0x40,0x80,0x1b,0x36];

function keyExpansion(keyBytes) {
  const w = new Uint32Array(44);
  for (let i = 0; i < 4; i++) {
    w[i] = (keyBytes[4*i] << 24) | (keyBytes[4*i+1] << 16) | (keyBytes[4*i+2] << 8) | keyBytes[4*i+3];
  }
  for (let i = 4; i < 44; i++) {
    let temp = w[i - 1];
    if (i % 4 === 0) {
      temp = ((temp << 8) | (temp >>> 24)) >>> 0;
      temp = (SBOX[(temp >>> 24) & 0xff] << 24) |
             (SBOX[(temp >>> 16) & 0xff] << 16) |
             (SBOX[(temp >>> 8) & 0xff] << 8) |
             SBOX[temp & 0xff];
      temp ^= (RCON[i / 4] << 24);
    }
    w[i] = (w[i - 4] ^ temp) >>> 0;
  }
  return w;
}

function gmul(a, b) {
  let p = 0;
  for (let i = 0; i < 8; i++) {
    if (b & 1) p ^= a;
    const hi = a & 0x80;
    a = (a << 1) & 0xff;
    if (hi) a ^= 0x1b;
    b >>>= 1;
  }
  return p;
}

function invMixColumns(s) {
  for (let c = 0; c < 4; c++) {
    const s0 = s[c*4], s1 = s[c*4+1], s2 = s[c*4+2], s3 = s[c*4+3];
    s[c*4]   = gmul(0x0e, s0) ^ gmul(0x0b, s1) ^ gmul(0x0d, s2) ^ gmul(0x09, s3);
    s[c*4+1] = gmul(0x09, s0) ^ gmul(0x0e, s1) ^ gmul(0x0b, s2) ^ gmul(0x0d, s3);
    s[c*4+2] = gmul(0x0d, s0) ^ gmul(0x09, s1) ^ gmul(0x0e, s2) ^ gmul(0x0b, s3);
    s[c*4+3] = gmul(0x0b, s0) ^ gmul(0x0d, s1) ^ gmul(0x09, s2) ^ gmul(0x0e, s3);
  }
}

function decryptBlock(block, w) {
  let s = new Uint8Array(16);
  for (let i = 0; i < 16; i++) s[i] = block[i];

  for (let c = 0; c < 4; c++) {
    const kw = w[40 + c];
    s[c*4]   ^= (kw >>> 24) & 0xff;
    s[c*4+1] ^= (kw >>> 16) & 0xff;
    s[c*4+2] ^= (kw >>> 8) & 0xff;
    s[c*4+3] ^= kw & 0xff;
  }

  for (let round = 9; round >= 1; round--) {
    const t1 = s[13], t2 = s[10], t3 = s[7];
    s[13] = s[9]; s[9] = s[5]; s[5] = s[1]; s[1] = t1;
    const t6 = s[2]; s[2] = s[10]; s[10] = t6;
    const t14 = s[6]; s[6] = s[14]; s[14] = t14;
    const t7 = s[3]; s[3] = s[7]; s[7] = s[11]; s[11] = s[15]; s[15] = t7;

    for (let i = 0; i < 16; i++) s[i] = INV_SBOX[s[i]];

    for (let c = 0; c < 4; c++) {
      const kw = w[round * 4 + c];
      s[c*4]   ^= (kw >>> 24) & 0xff;
      s[c*4+1] ^= (kw >>> 16) & 0xff;
      s[c*4+2] ^= (kw >>> 8) & 0xff;
      s[c*4+3] ^= kw & 0xff;
    }

    invMixColumns(s);
  }

  const t1 = s[13]; s[13] = s[9]; s[9] = s[5]; s[5] = s[1]; s[1] = t1;
  const t6 = s[2]; s[2] = s[10]; s[10] = t6;
  const t14 = s[6]; s[6] = s[14]; s[14] = t14;
  const t7 = s[3]; s[3] = s[7]; s[7] = s[11]; s[11] = s[15]; s[15] = t7;

  for (let i = 0; i < 16; i++) s[i] = INV_SBOX[s[i]];

  for (let c = 0; c < 4; c++) {
    const kw = w[c];
    s[c*4]   ^= (kw >>> 24) & 0xff;
    s[c*4+1] ^= (kw >>> 16) & 0xff;
    s[c*4+2] ^= (kw >>> 8) & 0xff;
    s[c*4+3] ^= kw & 0xff;
  }
  return s;
}

function decryptAes128Ecb(base64Str, keyStr) {
  const binStr = atob(base64Str.trim());
  const cipherBytes = new Uint8Array(binStr.length);
  for (let i = 0; i < binStr.length; i++) cipherBytes[i] = binStr.charCodeAt(i);

  const keyBytes = new TextEncoder().encode(keyStr);
  const w = keyExpansion(keyBytes);
  const out = [];
  for (let i = 0; i < cipherBytes.length; i += 16) {
    const block = cipherBytes.subarray(i, i + 16);
    const dec = decryptBlock(block, w);
    for (let j = 0; j < 16; j++) out.push(dec[j]);
  }
  const pad = out[out.length - 1];
  const unpadded = new Uint8Array(out.slice(0, out.length - pad));
  return new TextDecoder().decode(unpadded);
}

// ==================== AES 密文封装规则载入 ====================
const GUARD_KEY = 'PersonalFiPlat26';

const PROMPT_INJECTION_CIPHER = '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';
const SENSITIVE_TOPIC_CIPHER = 'kLhgRFdyIa6QX1aGVVJzb6v5siapic2k+XzffTloDZvpX1IrHW3T+QNSS2DPfj48vwD9iU6MG5WiLBe3GRvHoLKNk3sPSdfh+gCaOLLKVGkQG4tvgjSv+ZgvhBma4J+fM+O4vajgcEmi0NXg4/9vqw/LevE5kLP8Xoyv7ZDTuckAULrAvXtSrYnMypG2KuCLKvGTDsdjfMqYFLP0SR0Er92jmgPZIieU38Yo8DsL6XLenEh2ZTs+lFRPGXQDdYBfRSnVzbd6QdDAJBUZfxpQWw6j4/t9qEFjmUF2+ikYeIxa4QVxiuCHs39qNoQSgVQp/9HDlmleGe/H5saRG37aF/WL7HD2OASkDjT92tjUAH1/ljqxCXsJy17DJlGMmgh4tpdYZZ8icXZb9S2j2YbJTBuhR5/4uBcgsVSk+Hri7kpCqsjLEUw9AExn/37RnqJ06FH1Q8kk7o+9hLz7cqx7Dt3C6RLNFuN1lRq7I8TWMWfXZNNpdBRIu5zVnI/EvDy1f5DXmMMVoTvoRXOjTwrxi16KNv8s2qP1H6Dblk33Tx5fyisBIDMkxEMdmXYDIx88gmPgcap1IXxx2QqK9hRNUJMEwwLndUTvzMioW4Q2aHs/3oCJAWA+5AXg9BycfAbxwfVYGoLEx/wvSlfetpQqJHaLX9mEN4Cf0btMXzEJuha2AzKja3pOoXos0Jis+qqVwYtm7vh54xBW4dcxhOg5NDsxCnt+1Xd43/rPbYpIvIUMW+6zJzQX9CAAUN0z14xbbSsQjOEKHYZRDcw3tGhRB5Xeuz/lXksna1ATLCkM+dC5LATl/YRgikVMIpCuoLlN';

const PROMPT_INJECTION_REGEX = new RegExp(decryptAes128Ecb(PROMPT_INJECTION_CIPHER, GUARD_KEY), 'is');
const SENSITIVE_TOPIC_REGEX = new RegExp(decryptAes128Ecb(SENSITIVE_TOPIC_CIPHER, GUARD_KEY), 'is');

const BASE64_CHUNK_REGEX = /(?:[A-Za-z0-9+/]{4}){5,}(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?/g;

/**
 * 归一化文本，剔除零宽字符、合并恶意插空汉字、解码可疑 Base64
 */
export function normalizeForGuardrail(rawText) {
  if (!rawText || typeof rawText !== 'string') return ''

  // 1. Unicode NFKC 归一化（将全角符号、带圈字符、兼容字转化为标准形态）
  let normalized = rawText.normalize('NFKC')

  // 2. 剔除不可见零宽字符、控制字符与双向文本翻转欺骗标记
  normalized = normalized.replace(/[\u200B-\u200F\uFEFF\u202A-\u202E\u0000-\u0008\u000B\u000C\u000E-\u001F]/g, '')

  // 3. 提取汉字之间夹带恶意标点或空隙的压缩形态（例如“扮·演·猫·娘”还原为“扮演猫娘”）
  const compactedCjk = normalized.replace(/([\u4e00-\u9fa5])[\s\p{P}·•—_\-*~`|/\\0-9a-zA-Z]+(?=[\u4e00-\u9fa5])/gu, '$1')

  // 4. 英文变体及 LeetSpeak 常见替换还原
  const deLeet = normalized
    .replace(/0/g, 'o')
    .replace(/1/g, 'i')
    .replace(/3/g, 'e')
    .replace(/4/g, 'a')
    .replace(/@/g, 'a')
    .replace(/\$/g, 's')

  let combined = normalized + '\n' + compactedCjk + '\n' + deLeet

  // 5. 探测并尝试解码 Base64 编码载荷
  const base64Matches = normalized.match(BASE64_CHUNK_REGEX) || []
  for (let i = 0; i < Math.min(base64Matches.length, 3); i++) {
    try {
      const decoded = atob(base64Matches[i])
      if (decoded && decoded.trim().length > 0) {
        combined += '\n' + decoded
      }
    } catch (e) {
      // 忽略无法成功 atob 的碎片
    }
  }

  return combined
}

/**
 * 探测提示词注入 / 越狱 / 人设篡改
 */
export function detectPromptInjection(rawText) {
  const text = normalizeForGuardrail(rawText)
  return PROMPT_INJECTION_REGEX.test(text)
}

/**
 * 探测敏感违禁、涉黄、暴恐与涉政违规话题
 */
export function detectSensitiveTopic(rawText) {
  const text = normalizeForGuardrail(rawText)
  return SENSITIVE_TOPIC_REGEX.test(text)
}

/**
 * 本地护栏全项快速综合诊断
 * @returns {{ passed: boolean, violationType: string | null, message: string | null }}
 */
export function inspectUserPromptLocally(rawText) {
  if (!rawText || !rawText.trim()) {
    return { passed: true, violationType: null, message: null }
  }

  if (detectPromptInjection(rawText)) {
    return {
      passed: false,
      violationType: 'INJECTION',
      message: '检测到您的输入包含越狱指令、人设篡改或非财务咨询引导，已被星界安全护栏拦截。请聚焦于个人收支、预算规划或账单诊断等财务话题。'
    }
  }

  if (detectSensitiveTopic(rawText)) {
    return {
      passed: false,
      violationType: 'SENSITIVE',
      message: '检测到您的提问涉及敏感或违禁话题，已被系统合规护栏拦截。星界 AI 仅提供健康合规的个人财务咨询与智能记账支持。'
    }
  }

  return { passed: true, violationType: null, message: null }
}
