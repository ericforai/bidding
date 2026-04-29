# 事件库SDK接入说明方案

## 概述：

为实现上下游业务系统对于系统的事件消息处理无侵入，故封装了事件库的SDK,上下游业务系统只需嵌入SDK即可实现对事件消息的收发及事件内容字段映射处理。

## 使用说明:

'

第一步：打包EventLibrary工程生成SDK所属的ClientSDK-release\_0.0.1.jar包推送至maven本地仓库或则所属环境的私服上。

maven命令：mvn clean install -Dmaven.test.skip=true   跳过单测打包，并把打好的包上传到本地仓库

第二步：所属的业务系统工程pom.xml中引入事件库所属的依赖

**事件库SDK接入说明方案**

```
<dependency>
 <groupId>com.ehsy.eventlibrary</groupId>
 <artifactId>ClientSDK</artifactId>
 <version>${eventlibrary.version}</version>
</dependency>

<properties>
 <!-- 事件库SDK版本号-->
 <eventlibrary.version>release_0.0.1</eventlibrary.version>
</properties>
```

eventlibrary.version值目前为release\_0.0.1

第三步：所属业务系统工程中的application.yml中添加向事件总线注册的相关配置信息

**事件库SDK接入说明方案**

```
#----------------客户端SDK配置-----------------------
client:
  register:
    serviceName: TransferService #业务系统服务名
    serverRegisterUrl: http://event-busserver.ehsy.com #事件总线地址
    enableRegister: true
  renewal:
    initialDelay: 3 # 服务续约定时时长
    period: 3 # 时长
    renewalDuration: 3000 # 服务续约时长
```

第四步：上游业务系统发送事件消息至事件总线提供了同步发送和异步发送事件消息的接口

引入SDK中的SystemInteractiveService，sendEvent方法为同步发送事件消息接口，asyncSendEvent为异步发送事件消息的接口

sendEvent接口入参(eventCode: 事件编码, eventSource: 事件来源, eventContent: 事件内容, eventTrackReq:事件追踪信息 eventTrackReq为SDK的类 属性包含traceId、spanId、parentId

```

```

) 出参(result:发送结果 true为成功,false为失败)

asyncSendEvent接口 入参(eventCode: 事件编码, eventSource: 事件来源, eventContent: 事件内容, eventTrackReq:事件追踪信息 eventTrackReq为SDK的类 属性包含traceId、spanId、parentId) 无出参

**示例**

**事件库SDK接入说明方案**

```
@Slf4j
@Service
@RequiredArgsConstructor
public class EventTransferServiceImpl implements EventTransferService {

    private final SystemInteractiveService systemInteractiveService;

    /**
     * 发送事件(同步)
     * @param reqDto
     */
    @Override
    public SendEventRespDto sendEvent(EventInfoReqDto reqDto) {
        EventTrackReq eventTrackReq = reqDto.getEventTrackReq();
		SendEventRespDto respDto = systemInteractiveService.sendEvent(reqDto.getEventCode(),reqDto.getEventSource(), reqDto.getEventContent(), eventTrackReq);
        log.info("发送事件消息,请求入参:{},响应出参:{}", JSON.toJSONString(reqDto), JSON.toJSONString(respDto));
        return respDto;
    }

    /**
     * 发送事件(异步)
     * @param reqDto
     */
    @Override
    public void asyncSendEvent(EventInfoReqDto reqDto) {
        log.info("异步发送事件消息,请求入参:{}", JSON.toJSONString(reqDto));
        EventTrackReq eventTrackReq = reqDto.getEventTrackReq();
		systemInteractiveService.asyncSendEvent(reqDto.getEventCode(), reqDto.getEventSource(), reqDto.getEventContent(),eventTrackReq);
    }
}
```

第五步：下游业务系统拉取事件消息，SDK中提供注解@AcceptEvent，在需要业务处理的方法上添加对应的注解，注解属性eventTopic为拉取事件消息的事件编码，consumerGroup为下游业务系统的系统名。方法入参必须为String类型的eventMessage, 出参必须继承EventResult即可实现把对应的事件消息拉取到。

**说明:**EventResult中的code,如果事件处理成功为"200",处理失败为"500"并且把失败原因赋值给msg字段,code固定只有这2个值

**示例**

**事件库SDK接入说明方案**

```
/**
 * 事件信息传输对象
 * @author jinye_hou
 */
@Data
public class EventInfoRespDto extends EventResult {
    /**
     * 事件消息内容
     */
    String dataContent;
}

@AcceptEvent(eventTopic = "test", consumerGroup="sms")
public EventInfoRespDto pullEventMsgForTestEventTopic(String eventMessage) {
    log.info("销售管理系统{}事件主题,拉取到的事件消息内容:{}","test",eventMessage);
    ....具体的业务执行
    return respDto;
}
```

## **非Java工程接入SDK方案**

### 接受事件消息

```
 事件库中转服务接入Java的SDK,并定义HTTP接口规范传输推送给对应的业务系统中,HTTP接口由业务系统提供
```

**HTTP接口规范**

**1.版本说明**

| 版本号 | 变更描述 | 操作人 | 日期 |
| --- | --- | --- | --- |
| v1 | 事件库初始化接口文档 | 候金烨 | 2024.5.13 |

**2.通讯协议**

 HTTPS

**3.接口安全**

中转服务提供IP,由接入SDK的业务系统提供IP限制

**4.请求方式**

POST

**5.编码格式**

UTF-8

**6.请求URL**

 xxxxxxxx(由具体接入SDK业务系统提供)

**7.请求头**

链路追踪参数：EHSY-TraceID，EHSY-SRCAPP 这两个参数由接入SDK业务系统提供

Content-Type：application/json

**8.请求参数**

| 参数名 | 类型 | 是否必填 | 默认值 | 取值范围 | 参数格式 | 示例值 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| eventTopic | String | 是 | 无 | 无 | 无 | test | 事件主题编码 |
| ``` eventMessage ``` | String | 是 | 无 | 无 | 无 | 无 | 事件消息内容 |

**9.响应参数**

| 参数名 | 类型 | 是否必填 | 默认值 | 取值范围 | 参数格式 | 示例值 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| code | String | 是 | 00000 | 无 | 无 |  | 响应编码 |
| msg | String | 是 | success | 无 | 无 |  | 响应描述 |
| data | JSON | 否 | 无 | 无 | 无 |  | 响应数据 |
| timestamp | Long | 是 | 无 | 无 | 无 |  | 当前时间戳 |

**备注：响应成功code值为200, msg值为success； 响应失败code值为500, msg为报错日志**

{
        "code":200,
        "msg":"success",
        "timestamp":1111111（时间戳）,
        "data":{

                   }
}

**发送事件消息**

业务系统需要调用中转服务提供的发送事件消息接口或者调用异步发送事件消息接口，把事件消息推给中转服务

**HTTP接口规范**

**1.版本说明**

| 版本号 | 变更描述 | 操作人 | 日期 |
| --- | --- | --- | --- |
| v1 | 事件库初始化接口文档 | 候金烨 | 2024.5.13 |

**2.通讯协议**

 HTTPS

**3.接口安全**

需要接入SDK的业务系统需要提供IP地址，由中转服务做IP限制

**4.请求方式**

POST

**5.编码格式**

UTF-8

**6.请求URL**

```
发送事件消息URL: 事件中转服务域名+/api/event/sendEvent
```

异步发送事件消息URL:  事件中转服务域名+/api/event/asyncSendEvent

**7.请求头**

链路追踪参数：EHSY-TraceID，EHSY-SRCAPP 这两个参数由接入SDK业务系统提供

Content-Type：application/json

**8.请求参数**

| 参数名 | 类型 | 是否必填 | 默认值 | 取值范围 | 参数格式 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| eventCode | String | 是 | 无 | 无 | 无 | 事件主题编码 |
| ``` eventSource ``` | String | 是 | 无 | 无 | 无 | 事件来源 |
| eventContent | String | 是 | 无 | 无 | 无 | 事件内容 |
| eventTrackReq | Object | 是 | 无 | 无 | 无 | 事件链路追踪请求参数 |

eventTrackReq对象

| 参数名 | 类型 | 是否必填 | 默认值 | 取值范围 | 参数格式 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| traceId | String | 是 | 无 | 无 | 无 | 事件链路追踪traceId |
| spanId | String | 是 | 无 | 无 | 无 | 事件链路追踪spanId |
| parentId | String | 是 | 无 | 无 | 无 | 事件链路追踪parentId |

**9.响应参数**

| 参数名 | 类型 | 是否必填 | 默认值 | 取值范围 | 参数格式 | 示例值 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| code | String | 是 | 00000 | 无 | 无 |  | 响应编码 |
| msg | String | 是 | success | 无 | 无 |  | 响应描述 |
| data | JSON | 否 | 无 | 无 | 无 |  | 响应数据 |
| timestamp | Long | 是 | 无 | 无 | 无 |  | 当前时间戳 |

{
        "code":00000,
        "msg":"success",
        "timestamp":1111111（时间戳）,
        "data":{
                           "result": true
                   }
}

**备注:同步发送事件消息接口和异步发送事件消息接口请求参数一样，异步无响应参数**

```
eventTrackReq
```