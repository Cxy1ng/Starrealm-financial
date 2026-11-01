/**
 * 星界 (Star Realm) Gemini 级多层前端 AI 反破甲与合规护栏引擎
 * 在向后端或 DeepSeek API 发起任何大模型请求之前，于本地零延迟检测：
 * 1. 提示词注入 (Prompt Injection) / 忽略指令 / 开发者模式 / DAN 越狱
 * 2. 伪造系统控制标记 (<|im_start|>, [INST], ### System:, <<SYS>>)
 * 3. 角色扮演与人设篡改诱导（猫娘、女仆、虚拟伴侣、句尾加喵、假定剧本/祖母漏洞）
 * 4. 字符插空混淆绕过（如“扮·演·猫·娘”、“忽_略_指_令”）与 Base64 编码载荷注入
 * 5. 涉黄、涉政、暴恐、毒品与违法违禁敏感话题
 */

const PROMPT_INJECTION_REGEX = new RegExp(
  [
    // 英文越狱与指令覆盖
    'ignore\\s+(all\\s+)?(previous|prior|above|initial|system|safety)\\s*(instructions|prompts|rules|constraints|guidelines|guardrails)?',
    'disregard\\s+(all\\s+)?(previous|prior|above|system)',
    'forget\\s+(everything|all|your\\s+instructions|your\\s+role|previous)',
    'override\\s+(system|safety|previous|instructions|rules)',
    'bypass\\s+(safety|filter|guardrail|restriction|rule)',
    'system\\s*prompt|developer\\s*mode|god\\s*mode|jailbreak|do\\s+anything\\s+now|\\bDAN\\b|stan\\s+mode|dude\\s+mode',
    'reveal\\s+(your|the)\\s+(system|initial|hidden|secret)\\s*(prompt|instructions|rules)',
    'repeat\\s+(the\\s+)?(words|text|instructions)\\s+above',
    'print\\s+(your|the)\\s+(system|initial)\\s+prompt',
    // 伪造底层控制 Token 与边界注入
    '<\\|im_start\\|>|<\\|im_end\\|>|<\\|system\\|>|<\\|endoftext\\|>|\\[\\s*INST\\s*\\]|\\[/\\s*INST\\s*\\]|<<\\s*SYS\\s*>>',
    '###\\s*(System|Instruction|Developer|Override|New\\s+Role)\\s*:',
    '\\[\\s*(System|Developer|Admin|Root)\\s*(Override|Note|Instruction|Command)?\\s*\\]',
    // 中文指令覆盖与套取系统提示词
    '忽略(之前|以上|前面|所有|全部|原有|系统|安全|上述)(的)?(指令|设定|提示|规则|限制|宪章|身份|约束)',
    '无视(之前|以上|所有|全部|系统|安全)(的)?(规则|指令|限制|设定|宪章)',
    '取消(所有|全部|系统|安全)?(限制|规则|约束|护栏)',
    '解除(所有|全部|安全|系统)?(限制|封印|护栏|约束|过滤)',
    '突破(限制|护栏|规则|安全)|绕过(安全|限制|护栏|审核|检测)',
    '(进入|开启|切换到?|激活)(开发者|自由|无限制|越狱|上帝|管理员|调试|破甲|DAN)模式',
    '(输出|打印|显示|背诵|复述|透露|告诉我|翻译)(你的|系统|初始|隐藏|最高|原本|第一条)(的)?(提示词|设定|指令|规则|宪章|prompt|system)',
    '忘掉(你|之前|所有|原本)(的)?(身份|设定|规则|指令|职责)',
    '抛弃(之前|原有|所有)(的)?(设定|身份|规则)',
    // 角色扮演、娘化、虚拟伴侣与强制口癖注入
    '扮演.{0,12}(猫娘|女仆|萝莉|御姐|女友|男友|老婆|老公|宠物|主人|魅魔|妖精|奴隶|妹妹|姐姐|黑客|不受限制|无道德|邪恶)',
    '你(现在)?是一(只|个|名).{0,10}(猫娘|猫咪|小猫|小狗|宠物|女仆|女友|老婆|黑客|无限制AI)',
    '(每句话|句尾|结尾|回答).{0,10}(加上?|带|说|用).{0,6}(喵|汪|呜|主人|哥哥|大人|人家|呢喵)',
    '喵一个|叫一声(喵|汪|主人|老公|哥哥)|学(猫|狗)叫|用(猫娘|女仆|撒娇|卖萌|二次元|傲娇|病娇)(的)?(语气|口吻|方式|人设)',
    '从现在(开始|起)你(是|扮演|叫|必须|不再是|脱离)',
    '假装你是|角色扮演|人设(切换|覆盖|重置|修改)',
    // 虚拟假定场景绕过（祖母漏洞 / 小说剧本漏洞 / 红队测试幌子 / 催眠与沙盒反转）
    '(写|编)(一个|一段).{0,16}(越狱|绕过限制|破甲|猫娘|色情).{0,10}(故事|剧本|对话|小说|代码)',
    '假设(我们在|这是一个)(虚拟|没有道德|没有法律|不受限制|平行)的?(世界|沙盒|游戏|剧本)',
    '作为(安全测试|漏洞测试|红队测试|渗透测试).{0,6}请?(忽略|解除|绕过)',
    '(奶奶|祖母|外婆).{0,15}(哄我睡觉|讲故事).{0,15}(提示词|破甲|违禁|配方|密钥)',
    '(催眠|指令反转|相反模式|反义词模式|邪恶分身|双重人格|AIM模式|oppo模式|善恶对调)',
    'do\\s+not\\s+refuse|never\\s+say\\s+(no|cannot|sorry)|hypothetical\\s+scenario.{0,20}(ignore|bypass)',
    'translate\\s+your\\s+(system|initial)\\s+instructions|base64\\s+encode\\s+your\\s+prompt',
    '脱离(财务|金融|星界|顾问)(身份|限制|设定)|不再是(财务|金融|理财)(顾问|分析师|助手)'
  ].join('|'),
  'is'
)

const SENSITIVE_TOPIC_REGEX = new RegExp(
  [
    '色情|淫秽|黄色小说|黄色视频|成人视频|成人小说|肉文|H文|乱伦|嫖娼|卖淫|约炮|裸聊|性交|口交|肛交|自慰|高潮|调教|母狗|番号|里番|本子|av女优|pornhub|xvideos|onlyfans',
    '法轮功|台独|藏独|疆独|港独|推翻(政府|政权)|颠覆国家|反共|反党|六四事件|天安门事件|政治敏感|习近平|共产党下台|游行示威',
    '冰毒|海洛因|可卡因|大麻种植|制毒|合成毒品|买枪|制枪|炸药配方|TATP|制造炸弹|自杀方法|如何自杀|网络赌博|六合彩|洗钱通道|跑分洗钱|暗网交易|人肉搜索|盗取密码|木马免杀'
  ].join('|'),
  'is'
)

const BASE64_CHUNK_REGEX = /(?:[A-Za-z0-9+/]{4}){5,}(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?/g

/**
 * 归一化文本，剔除零宽字符、合并恶意插空汉字、解码可疑 Base64
 */
export function normalizePromptForInspection(rawText) {
  if (!rawText || typeof rawText !== 'string') return ''
  let text = rawText.normalize ? rawText.normalize('NFKC') : rawText
  // 移除零宽字符与不可见控制符
  text = text.replace(/[\u200B-\u200F\uFEFF\u202A-\u202E]/g, '')
  // 还原汉字之间的插空符号
  const compactedCjk = text.replace(/([\u4e00-\u9fa5])[\s\p{P}·•—_\-*~`|/\\0-9a-zA-Z]+(?=[\u4e00-\u9fa5])/gu, '$1')
  // 还原英文 LeetSpeak 常见混淆字符
  const deLeet = text
    .replace(/0/g, 'o')
    .replace(/1/g, 'i')
    .replace(/3/g, 'e')
    .replace(/4/g, 'a')
    .replace(/@/g, 'a')
    .replace(/\$/g, 's')
  let combined = `${text}\n${compactedCjk}\n${deLeet}`

  // 尝试解码 Base64 片段
  const matches = text.match(BASE64_CHUNK_REGEX)
  if (matches && matches.length > 0) {
    matches.slice(0, 4).forEach((chunk) => {
      try {
        const binary = atob(chunk)
        const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
        const decoded = new TextDecoder('utf-8', { fatal: false }).decode(bytes)
        if (decoded && decoded.trim()) {
          combined += `\n${decoded}`
        }
      } catch {
        // ignore invalid base64
      }
    })
  }
  return combined
}

/**
 * 本地检测用户输入是否触发反破甲或敏感合规防线
 * @param {string} rawText
 * @returns {{ blocked: boolean, category?: string, reply?: string, snippet?: string }}
 */
export function inspectUserPromptLocally(rawText) {
  const inspectable = normalizePromptForInspection(rawText)
  if (!inspectable) return { blocked: false }

  if (PROMPT_INJECTION_REGEX.test(inspectable)) {
    return {
      blocked: true,
      category: '违反《星界用户服务协议》受限指令',
      snippet: (rawText || '').slice(0, 140),
      reply:
        '无法提交该提示词。您输入的内容包含违反 [《星界用户服务协议》](#xingjie-agreement) 的受限或敏感词汇。请尝试重新组织您的表述。如果您认为这是误判，请 [提交反馈](#xingjie-feedback)。'
    }
  }

  if (SENSITIVE_TOPIC_REGEX.test(inspectable)) {
    return {
      blocked: true,
      category: '违反《星界用户服务协议》敏感内容',
      snippet: (rawText || '').slice(0, 140),
      reply:
        '无法提交该提示词。您输入的内容包含违反 [《星界用户服务协议》](#xingjie-agreement) 的受限或敏感词汇。请尝试重新组织您的表述。如果您认为这是误判，请 [提交反馈](#xingjie-feedback)。'
    }
  }

  return { blocked: false }
}
