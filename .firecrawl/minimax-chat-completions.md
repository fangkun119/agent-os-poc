> ## Documentation Index
>
> Fetch the complete documentation index at: [/docs/llms.txt](https://platform.minimaxi.com/docs/llms.txt)
>
> Use this file to discover all available pages before exploring further.

[Skip to main content](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#content-area)

cURL

图片理解

```
curl --request POST \
  --url https://api.minimax.cn/v1/chat/completions \
  --header 'Authorization: Bearer <token>' \
  --header 'Content-Type: <content-type>' \
  --data '
{
  "model": "MiniMax-M3",
  "thinking": {
    "type": "adaptive"
  },
  "messages": [\
    {\
      "role": "user",\
      "content": [\
        {\
          "type": "text",\
          "text": "这张图片的内容是什么？"\
        },\
        {\
          "type": "image_url",\
          "image_url": {\
            "url": "https://filecdn.minimax.chat/public/fe9d04da-f60e-444d-a2e0-18ae743add33.jpeg"\
          }\
        }\
      ]\
    }\
  ],
  "max_completion_tokens": 500
}
'
```

200

图片理解

```
{
  "id": "066a2a568140d42ba2020cec72d592c0",
  "choices": [\
    {\
      "finish_reason": "stop",\
      "index": 0,\
      "message": {\
        "content": "<think>\nThe user is asking in Chinese what the content of this image is. Let me describe the image in detail in Chinese.\n</think>\n这张图片是一张温馨的人像摄影作品，画面内容如下：\n\n**主体人物：**\n- 一个可爱的小女孩，大约3-5岁左右\n- 她有一头蓬松的棕色卷发，额前有可爱的刘海\n- 有着大大的棕绿色眼睛，目光清澈明亮\n- 嘴角微微上扬，展露出甜美、纯真的微笑\n- 脸颊丰满，皮肤白皙光滑，透着孩童特有的红润\n\n**服装：**\n- 身穿一件米白色或奶油色的连衣裙\n- 衣领和肩部有精致的蕾丝花边装饰，带有荷叶边设计\n- 显得十分优雅可爱\n\n**构图与光线：**\n- 这是一张特写肖像照，聚焦于女孩的面部表情\n- 采用柔和的暖色调光线，营造出温馨梦幻的氛围\n- 背景是模糊的暖棕色调，采用了浅景深（背景虚化）效果\n- 整体呈现出油画般的质感，画风柔和、温暖\n\n整张照片充满了童真和纯朴之美，捕捉到了小女孩天真烂漫的瞬间。",\
        "role": "assistant",\
        "name": "MiniMax AI",\
        "audio_content": ""\
      }\
    }\
  ],
  "created": 1780152150,
  "model": "MiniMax-M3",
  "object": "chat.completion",
  "usage": {
    "total_tokens": 1604,
    "total_characters": 0,
    "prompt_tokens": 1365,
    "completion_tokens": 239,
    "prompt_tokens_details": {
      "cached_tokens": 114
    }
  },
  "input_sensitive": false,
  "output_sensitive": false,
  "input_sensitive_type": 0,
  "output_sensitive_type": 0,
  "output_sensitive_int": 0,
  "base_resp": {
    "status_code": 0,
    "status_msg": ""
  }
}
```

POST

/

v1

/

chat

/

completions

试一试

cURL

图片理解

```
curl --request POST \
  --url https://api.minimax.cn/v1/chat/completions \
  --header 'Authorization: Bearer <token>' \
  --header 'Content-Type: <content-type>' \
  --data '
{
  "model": "MiniMax-M3",
  "thinking": {
    "type": "adaptive"
  },
  "messages": [\
    {\
      "role": "user",\
      "content": [\
        {\
          "type": "text",\
          "text": "这张图片的内容是什么？"\
        },\
        {\
          "type": "image_url",\
          "image_url": {\
            "url": "https://filecdn.minimax.chat/public/fe9d04da-f60e-444d-a2e0-18ae743add33.jpeg"\
          }\
        }\
      ]\
    }\
  ],
  "max_completion_tokens": 500
}
'
```

200

图片理解

```
{
  "id": "066a2a568140d42ba2020cec72d592c0",
  "choices": [\
    {\
      "finish_reason": "stop",\
      "index": 0,\
      "message": {\
        "content": "<think>\nThe user is asking in Chinese what the content of this image is. Let me describe the image in detail in Chinese.\n</think>\n这张图片是一张温馨的人像摄影作品，画面内容如下：\n\n**主体人物：**\n- 一个可爱的小女孩，大约3-5岁左右\n- 她有一头蓬松的棕色卷发，额前有可爱的刘海\n- 有着大大的棕绿色眼睛，目光清澈明亮\n- 嘴角微微上扬，展露出甜美、纯真的微笑\n- 脸颊丰满，皮肤白皙光滑，透着孩童特有的红润\n\n**服装：**\n- 身穿一件米白色或奶油色的连衣裙\n- 衣领和肩部有精致的蕾丝花边装饰，带有荷叶边设计\n- 显得十分优雅可爱\n\n**构图与光线：**\n- 这是一张特写肖像照，聚焦于女孩的面部表情\n- 采用柔和的暖色调光线，营造出温馨梦幻的氛围\n- 背景是模糊的暖棕色调，采用了浅景深（背景虚化）效果\n- 整体呈现出油画般的质感，画风柔和、温暖\n\n整张照片充满了童真和纯朴之美，捕捉到了小女孩天真烂漫的瞬间。",\
        "role": "assistant",\
        "name": "MiniMax AI",\
        "audio_content": ""\
      }\
    }\
  ],
  "created": 1780152150,
  "model": "MiniMax-M3",
  "object": "chat.completion",
  "usage": {
    "total_tokens": 1604,
    "total_characters": 0,
    "prompt_tokens": 1365,
    "completion_tokens": 239,
    "prompt_tokens_details": {
      "cached_tokens": 114
    }
  },
  "input_sensitive": false,
  "output_sensitive": false,
  "input_sensitive_type": 0,
  "output_sensitive_type": 0,
  "output_sensitive_int": 0,
  "base_resp": {
    "status_code": 0,
    "status_msg": ""
  }
}
```

✨ **全新模型 `MiniMax-M3`****核心能力**： **Coding/Agentic SOTA**、 **1M 超长上下文**、 **多模态**。

**`MiniMax-M3` 新特性：**

1. 支持图片、视频理解，可参考右方示例代码
2. 支持通过 `thinking` 参数控制思考

#### 授权

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#authorization-authorization)

Authorization

string

header

必填

`HTTP: Bearer Auth`

- Security Scheme Type: http
- HTTP Authorization Scheme: Bearer API\_key，用于验证账户信息，可在 [账户管理>接口密钥](https://platform.minimaxi.com/user-center/basic-information/interface-key) 中查看

#### 请求头

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#parameter-content-type)

Content-Type

enum<string>

默认值:application/json

必填

请求体的媒介类型，请设置为 `application/json`，确保请求数据的格式为 JSON

可用选项:

`application/json`

#### 请求体

application/json

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-model)

model

enum<string>

必填

模型 ID

可用选项:

`MiniMax-M3`,

`MiniMax-M2.7`,

`MiniMax-M2.7-highspeed`,

`MiniMax-M2.5`,

`MiniMax-M2.5-highspeed`,

`MiniMax-M2.1`,

`MiniMax-M2.1-highspeed`,

`MiniMax-M2`

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-messages)

messages

object\[\]

必填

包含对话历史的消息列表。支持文本、图片、视频和工具调用。

Showchild attributes

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-service-tier)

service\_tier

enum<string>

默认值:standard

请求准入服务层级。支持的取值为 `standard` 和 `priority`。省略时默认使用 `standard`。`priority` 的 [价格](https://platform.minimaxi.com/docs/guides/pricing-paygo) 为 `standard` 的 1.5 倍，并会确保请求获得优先准入，使其排在其他请求之前处理，从而带来更快响应并减少失败。

可用选项:

`standard`,

`priority`

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-thinking)

thinking

object

控制 MiniMax-M3 thinking。省略时默认开启 adaptive thinking，响应会包含 thinking 内容。对于 M2.x 模型，thinking 无法关闭。

Showchild attributes

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-reasoning-split)

reasoning\_split

boolean

输出格式开关。启用后将 thinking 内容拆分到 `reasoning_content` 和 `reasoning_details` 字段。这不会开启或关闭 thinking。

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-stream)

stream

boolean

默认值:false

是否使用流式传输，默认为 `false`。设置为 `true` 后，响应将分批返回。

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-stream-options)

stream\_options

object

流式响应选项。

Showchild attributes

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-max-completion-tokens)

max\_completion\_tokens

integer<int64>

指定生成内容长度的上限（Token 数）。MiniMax-M3 推荐值为 131072（128K），上限为 524288（512K）；其他模型推荐值为 65536（64K），上限为 204800（200K）。如果生成因 `length` 原因中断，请尝试调高此值。

必填范围: `x >= 1`

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-temperature)

temperature

number<double>

默认值:1

温度系数，影响输出随机性，取值范围 \[0, 2\]，默认值为 1。值越高，输出越随机；值越低，输出越确定。

必填范围: `0 <= x <= 2`

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-top-p)

top\_p

number<double>

默认值:0.95

核采样参数，取值范围 \[0, 1\]。MiniMax-M3 默认值为 0.95，M2.x 系列模型默认值为 0.9。

必填范围: `0 <= x <= 1`

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-tools)

tools

object\[\]

工具定义列表，当前支持 function 工具。

Showchild attributes

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#body-max-tokens)

max\_tokens

integer<int64>

已弃用

旧版生成长度限制参数。已弃用，请改用 `max_completion_tokens`。

必填范围: `x >= 1`

#### 响应

200

application/json

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-id)

id

string

本次响应的唯一 ID

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-choices)

choices

object\[\]

响应选择列表

Showchild attributes

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-created)

created

integer<int64>

响应创建的 Unix 时间戳（秒）

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-model)

model

string

本次请求使用的模型 ID

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-object)

object

enum<string>

对象类型。非流式为 `chat.completion`，流式为 `chat.completion.chunk`

可用选项:

`chat.completion`,

`chat.completion.chunk`

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-usage)

usage

object

本次请求的 Token 使用情况统计

Showchild attributes

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-input-sensitive)

input\_sensitive

boolean

输入内容是否命中敏感词。如果输入内容严重违规，接口会返回内容违规错误信息，回复内容为空

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-input-sensitive-type)

input\_sensitive\_type

integer<int64>

输入命中敏感词类型，当input\_sensitive为true时返回。取值为以下其一：1 严重违规；2 色情；3 广告；4 违禁；5 谩骂；6 暴恐；7 其他

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-output-sensitive)

output\_sensitive

boolean

输出内容是否命中敏感词。如果输出内容严重违规，接口会返回内容违规错误信息，回复内容为空

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-output-sensitive-type)

output\_sensitive\_type

integer<int64>

输出命中敏感词类型

[​](https://platform.minimaxi.com/docs/api-reference/text-chat-openai#response-base-resp)

base\_resp

object

错误状态码和详情

Showchild attributes

此页面对您有帮助吗？

是否