ChoreographyESB
===============
本工具用于使用mule ESB和activiti BPM引擎来实现集成中多个消息流之间关系的规范化定义和运行时支持。我们把一个集成场景中多个相关的消息流定义为一个会话（conversation）。

该项目包含了如下集成会话中的整体约束示例：

1. 交互双方为A和B；
2. A发送请求至B，B进行回复；
3. 整体约束要求请求与回复消息之间间隔T时间以上。

![scenario](http://pic.yupoo.com/jjfd/CKDWi4OZ/medish.jpg "Conversation Scenario")

该示例包含如下实现元素：

1. 使用BPMN描述的mediation process；
2. 使用mule实现的两个inbound flow（flow 1 & flow 3），对外提供HTTP endpoint（endpoint 1 & 3），在flow中根据传入的消息触发mediation流程中的receive task。传入的消息包括A发出的请求消息和B发出的回应消息；
3. 实现传入消息与mediation流程中receive task映射的component（component 1 & 3）；
4. 实现流程中send task的outbound flow（flow 2 & flow 4），以java组件形式实现（component 2 & 4），通过mule client调用交互双方提供的交互接口，包括B提供的接收请求接口和A提供的接收响应接口；【可优化，比如实现为发送消息到mule的vm endpoint，然后定义outbound flow实现具体的发送】
5. 用于模拟A和B接收接口的两个flow，提供HTTP endpoint（endpoint 2 & 4）

![implementation](http://photo.yupoo.com/jjfd/CKDWyF5u/medish.jpg "impl")

#Conversation的由来：

1. SOA已经成为企业集成的主要方式，把各种系统封装为服务，实现这些服务之间的交互以及协同工作。随着集成从最简单的request-reply发展为更复杂的conversation，一些业务流程被封装为服务，而这些服务的实现依赖于多次相互关联的与服务调用者的消息交互。特别是通过组合原子服务得到的composite service之间的交互，涉及多次消息交互，也有可能涉及多个参与方。比如电子商务中的quote（问价）、order（订单）、shipment（发货）、invoice（发票）、payment（付款）、refund（退款）。
2. 医疗中的conversation：医疗IT环境中的各种系统或者科室都有自治的流程，病人的诊疗体现为这些流程之间的协作。系统之间的协作不再是简单的request-response，而是复杂的多次交互，有状态。要达到语义互操作，就要实现这些流程之间的互操作。
3. 医疗IT环境中的细粒度消息conversation，或者基于消息标准实现的服务，在完成一个特定场景任务过程中，会维持一个会话，比如一个申请单处理会话，包括申请、响应、状态通知、报告一系列消息交互，这些交互之间既有依赖又有约束，如果再考虑取消、变更或者其他异常，会形成非常复杂的会话。再比如BPMN标准中choreography的例子：病人和医生的交互；
4. conversation和choreography的共同立足点都是go beyond simple request-reply，只不过一个针对的是通用的消息交互，另一个关注service之间的消息交互。在下文中，conversation、choreography的意义是等同的。

![Conversation](http://photo.yupoo.com/jjfd/CJKhlnfd/medish.jpg "Conversation Sample")

可以看出，conversation和choreography是集成复杂度增加后必然要面对的问题。因此，为了有效的管理集成，需要对conversation（choreography）进行描述和实现。特别的，针对变化的需求，需要在中间件层进行快速的设计和配置，以使整体交互满足特定的需求。

#现存问题

* IHE中描述了医疗IT集成中常见的场景，其中包括大量的系统间会话，特别是workflow类型的profile。一个workflow的完成，依赖于各个系统在恰当的时间发送正确的信息给正确的接收者。然而，IHE采用文本和图形的方式描述conversation，缺乏灵活调整的能力。因此，需要一种建模方法能够对IHE中体现的choreography进行描述，并且这个建模方法中的元素可以调整以适应新的需求。
* SOA中的choreography可以描述服务之间的交互，标准语言包括WS-CDL、BPEL4chor和BPMN。其中WS-CDL和BPEL4chor与web service绑定，缺乏对医疗领域中消息标准的适应能力。BPMN具备图形化元素，而且接口实现可以扩展。
* 缺乏对choreography实现的支持（choreography与集成中间件缺乏联系）。最简单的choreography通过service之间自发的调用连接实现。这样的方式是decentralized。但问题是，service之间存在很高的耦合度。 引入集成产品后，在service之间形成proxy，用于解耦合，并提高灵活性。 另外，由于service实际提供的接口交互方式未必是与choreography设计完全吻合的，所以需要集成产品在service之间实现mediation。而现在缺乏确定的方式实现choreography模型与ESB实现之间的转换。
* 现有集成中间件对choreography的约束缺乏支持：conversation约束的例子：

    >    * 金融投资交互流程中的时间间隔约束：比如投资代理首先给客户发送一个消息，客户回复的消息需间隔24小时以上（由政府规定），否则该交易无效（Kopp文章中的例子）；
    >    * 放射申请单会话中的顺序约束：CIS会针对所有的状态更新消息变更状态，而RIS无法避免由于误操作导致的错误状态信息。比如即使CIS在已报告状态，如果又接收到检查完成消息，仍然会把状态变回检查完成，这会延误诊疗。又如，没有发检查完成（ORM-SC-CM）就发了报告（ORU），会导致无法获取影像，同样延误诊疗；
    >    * （ADT与ORM）

这些会话关系需要由集成中间件来管理，包括enactment或者检查violation。如果发生错误，可能会影响业务质量。如果不能实时发现并干预，也会造成一系列的错误。

因此，**研究问题**是：

>  如何采用规范化、模型化的方法描述医疗IT集成中的conversation，并且通过集成中间件（integration engine/ESB）提供支持。

***


#集成会话的描述

因此，我们首先的问题是：是否可以抽象出一种通用的结构化的方法来描述这样的conversation（同一conversation中消息之间的关系）？如何使用集成中间件的架构来支持这样的结构化描述的实现？

##conversation的已有规范化描述方式

* 枚举：如WSDL定义中的message exchange pattern
* 单个参与者的外部行为描述（public，visible），如BPEL的abstract process、BPMN的public process
* global view：从整体上描述交互场景，包含多方。如WS-CDL、BPMN的choreography图、BPEL4chor等
* rules：采用declarative的方法声明消息之间的关系

根据这样的规范化描述，如何实现能管理conversation或choreography的ESB？

***

##BPMN4C：基于BPMN choreography模型的集成会话描述方法

为了规范化描述集成中的会话，我们对BPMN标准进行裁剪和扩展。使用BPMN作为模型基础的好处是：1）BPMN具备图形化元素，直观易理解；2）BPMN中已经定义了choreography与collaboration模型以及conversation模型之间的相互转换。

约束：
需要定义能唯一定位一个message flow的message定义。比如，如果把response分为reject和accept两个message flow，则需要区分两种消息类型。

* 使用BPMN choreography描述整体交互的约束。

投资的例子：

![Temporal Constaint Choreography](http://photo.yupoo.com/jjfd/CJbbBguf/medish.jpg" Temporal Constaint Choreography")

#集成会话的实现：

## conversation的实现方式：历史回顾
* 自发实现：依赖各自完全遵循global agreement，并且紧密耦合。
* 为了解耦合，引入中间件做proxy
* 为了解决异构性，需要mediation。以ESB为代表的集成产品成为实现系统和服务间消息交互的主要媒介
* conversation要解决消息通道之间的关联和相互约束

    * 简单直接的ad-hoc方式: [simple](https://github.com/nhcphdthesis/ChoreographyESB#conversation-4)
    * 对conversation进行描述并使用流程引擎管理的方式: [Kopp, et al.](https://github.com/nhcphdthesis/ChoreographyESB#koppchoreography-aware-esb)

##使用ESB支持BPMN4C的实现：

基本实现方式：

* 把BPMN choreography转化为orchestration流程或者规则。把实现每个message flow的channel看做BPMN 流程中的一个task，遵循choreography中定义的control-flow实现channel之间的逻辑。

实现示例：

在Activiti中的建模实现：

![Activiti mediation process](http://photo.yupoo.com/jjfd/CJbesoCC/medish.jpg "Activiti mediation process")

其中，几个message event分别引用几个message对象的id。

1. 先考虑web service形式。

- 该流程中receive task对应的operation指向ESB提供给sender的operation实现，send task对应的operation指向receiver提供给ESB的operation实现。
- event-based gateway用于根据不同receive task接收到消息而选择不同的branch。
- timer event用于延时。在timer时间窗中进入ESB的消息将无法找到对应的receive task

按照BPMN的定义，message event可以引用operation关联到web service的实现，send task也对应到期望发送的operation实现上。但是，activiti的eclipse编辑器中并未提供service task的属性定义方式。
【web service方式实现的好处，标准化，即使BPMN引擎或者ESB更换，也不影响已有接口；缺点：需要实现大量web service接口（由ESB实现服务，内部转换为与遗留系统的消息交互）】

2. 如果使用与mule集成的组件？（使用proprietary的接口方式）

- send就是向mule的endpoint发送消息。
- receive由mule设置variable后signal

这样，BPMN引擎与外部的交互全部通过mule代理。原来的sender和receiver之间的mule flow相当于拆分成两部分：

一部分是从sender接收到消息发送到BPMN引擎
比如该流程中的start event，在mule使用HTTP endpoint接收到来自consult的消息后，调用：

    RuntimeService.startProcessInstanceByMessage(String messageName);

这意味着，messageName必须在所有部署的流程里面唯一。如何知道一个消息实例对应流程定义中的哪个messageName？需要在接收到消息的inbound-endpoint和发送给activiti的component中，从一个数据表中查询当前channel中的数据类型与BPMN流程定义中message name的映射关系。然后查询是否有流程能以这个message来启动，如果有，调用上面的API。
如果没有，根据correlation定义从消息中抽取correlation id（流程id），查询当前该流程是否具备一个等待消息的receive task，如果有，调用API触发这个task。

    ProcessInstance pi = （查找到的实例）
    Execution execution = runtimeService.createExecutionQuery()
      .processInstanceId(pi.getId())
      .activityId("waitState")
      .singleResult();
    assertNotNull(execution);   
    runtimeService.signal(execution.getId());

另一部分是接收到从BPMN引擎来的数据发送到特定的receiver。
比如此例中start event之后的send task会发送消息到mule的一个vm，这个endpoint接收后调用customer提供的web service。

**总结：如何实现BPMN定义的choreography？**

- 把BPMN choreography模型转换为orchestration模型，每个interaction转换为一个receive-BPM和BPM-send对。
- 实现提供给sender的消息接收服务到BPMN引擎接收消息接口的flow
- 实现从BPMN引擎接收消息到receiver的flow
- 有一个dedicated component，用于实现消息与流程之间的关联，这个component，就是Kopp他们文章中提出的choreography manager
- 具备了这些结构的ESB，就是具备了conversation管理能力的ESB（或者，叫choreography-aware ESB）

**如何找到一个消息实例对应哪个choreography实例？**
我们假定在交互者之间已经定义了correlation方法，在choreography manager中需要维护各个business correlation与流程实例ID的correlation。

**找到了choreography实例后，如何判断是否有接收该消息实例的receive task实例？**
在实例中查找当前激活的task，过滤出receive task，再根据receive task关联的消息类型和correlation过滤。

这样的方法解决了Kopp他们方法的问题了吗（其他技术、图形化、重试时间）
-- 似乎现在只是实现了图形化描述。而correlation其实很难图形化，需要用模型定义。

***
##技术实现

我们设计一个mule的component，来实现如上choreography管理的功能：

![Choreography Manager](http://photo.yupoo.com/jjfd/CJTY9gHk/CisiU.png)

1. 首先从总线上接收一个消息实例；【比如一个检查状态消息】
2. 根据这个消息实例的correlation id，判断是否有BPMN流程实例与其关联；【根据检查状态中的order id，查找其对应的流程id，并到activiti中查找这个流程id的实例】
3. 根据这个消息实例的消息类型（evaluation表达式），判断关联的流程实例中是否有相应的receive task；【根据查询到的流程实例，获取其流程定义，查找其中是否有定义了接收该类型消息的receive task，然后检查当前实例的相应task是否在激活状态】
4. 若有，则把消息signal给相应的流程实例；【设置variable之后，signal】
5. 若没有，则判断是否有流程能被这个类型的消息实例启动；【查询所有部署的流程定义，是否有以】
6. 若以上判断都没有，说明该消息实例未在choreography设计预期内，路由给异常处理模块；

该component仅用作处理从接收消息端口到activiti的过程。从activiti发送到mule的消息根据预定义的规则发送给相应的receiver。


##医疗中的conversation模板和集成引擎支持




医疗IT集成场景中包含大量类似的conversation，比如以order为核心的conversation，出现在IHE的放射检查、病理、心电、实验室检验等profile中。形成一种非常接近的conversation pattern。
这些conversation pattern可以使用集成配置模板来支持，加快集成速度。

******

***异常处理***
按照上面的方法，当相应conversation中并不存接收当前消息实例的receive task时，即视为违反了conversation定义。那么这个消息如何处理？
  对于一个以imperative方式声明的choreography流程来说，可能的异常情况会非常多。比如对于IHE profile中的基本process flow来说，往往存在很多variation。对于SWF profile，最基本的process flow是administrative and procedural process flow，在其基础上，还有patient update、order update、order cancel、appointment等variation。如何制定这些异常的处理方法？特别的，哪些异常情况是预先定义的可以正常路由发送的，哪些异常情况是需要特殊处理的，哪些异常情况是无法估计依赖人工处理的？

- 对于expected exception，可以定义规则，触发规则后定义调用预定义的处理逻辑；
- 对于unexpected exception，路由到一个地方提醒人工处理。

**多个conversation的管理**

考虑放射检查会话，预定义的流程如下：

- CIS向RIS下达申请单申请
- RIS回应是否接受或拒绝
- 若接受，RIS向CIS不断发送状态更新

存在的几个variation：

- 更新申请单

    > - CIS更新申请单，若RIS遵从IHE，则先cancel，再new

- 取消申请单

    > - CIS取消申请单，异步等待取消申请单结果
    > - RIS接受取消申请单申请，异步通知状态更新（CA或原状态：不能取消）
    > - 若已经取消，则原来的conversation将终止

- 预约信息

    > - RIS对申请单进行预约，发送预约信息；
    > - RIS对预约进行更改，发送预约更新信息；
    > - RIS取消预约，发送预约取消信息
    
*设想一种情况，假如一个护士对一个申请单进行了预约，发送预约状态消息（message 1），而就在此时CIS下达了取消申请单的命令，RIS系统自动取消后通知CIS状态变更为取消（message 2），如果没有保证这两个消息发送的顺序，可能message 2先到达，然后message 1到达，则CIS端的状态出错。*


如何使用BPMN choreography描述？

最关键的是不同choreography中transaction之间的约束关系。关系类型包括：sequence、parallel、exclusive。

BPMN中，使用两个属性定义是否允许choreography模型以外的消息交互：

- isClosed。该collaboration属性定义整个choreography模型。若为true，则不允许participant之间传递choreography模型以外的消息
- isImmediate。该sequence flow属性定义两个choreography task之间是否允许其他消息。若为true，则允许。若isClosed为true，则该属性被override





*******

#更多……

##最简单、直接的方式（基于集成中间件）实现conversation约束
现有ESB没有直接支持conversation的能力（因为其基于的EIP本身是无状态的），需要定制开发。
  比如，在集成层，针对每个消息，查询与其关联的历史消息，并根据实际需求使用代码实现约束的violation检测或者异常处理。

这里面涉及的工作：

* correlation id的定义：针对消息制定表达式
* correlation的生成和维护：若消息本身不包含这个id，则需要由集成中间件生成并维护；
* 实际需求的描述和实现：需求类型：时间约束、顺序关系、……
* 与需求不一致时的处理方式：触发警报、按照需求处理、静默、忽略……

接收到消息后集成channel中做的事情：

1. 抽取correlation，
2. 查找实例，
3. 判断实例属性，
4. 处理。

以投资例子中的时间约束为例，A消息与B消息需间隔t小时。比如在mule flow中，使用一个component来实现这个逻辑：

1. 获取关联：首先根据correlation定义抽取出correlation id；
2. 查找：然后到消息历史中去查询。然后判断业务逻辑定义中是否有涉及该消息实例的定义。对于接收到的B消息实例，根据correlation id查找是否有具备同一correlation id的A消息实例。
3. 判断属性：查找A消息实例被实际发送到接收者的时间，并与接收到B消息实例的时间相比较；
4. 处理：如果时间间隔大于t小时，则正常路由，否则报警或者保存消息延迟发送（依赖于具体的处理方式定义）

如果是顺序约束，以放射科申请单为例。下达申请后，要求先发送检查完成状态变更，再发送报告状态。如果未发送检查完成，不能发送报告。如果已经发送了报告，则不能再发送检查完成。

以mule flow实现：

1. 第一个flow：从CIS到RIS的申请单传递；定义correlation id为申请单号。
2. 第二个flow：从RIS到CIS的状态变更，状态采用一个代码；
3. 加入一个component，获取其correlation id。查找是否有历史消息。
3.1. 如果消息实例的类型是检查完成，则判断是否已有报告状态的消息实例。如果有，则根据预定义的逻辑，忽略该消息。否则，直接送至CIS。
3.2. 如果类型是报告，则判断是否已有检查完成状态的消息实例。如果没有，则根据预定义的规则把消息实例保存。等t小时后，若仍然没有收到检查完成的消息更新，则根据报告状态的消息生成一条检查完成状态消息发送给CIS，然后发送该报告状态信息。如果在t小时之内有检查完成状态消息到达，则查找是否有缓存的报告消息并依次发送。

很不优雅。
这里，如果再叠加一个时间限制：检查完成和报告的状态之间不能小于5小时，不能大于48小时，如何处理？小于间隔时间的情况采用延时处理，大于间隔时间的通过一个另外的时间监听器实现，时间到时触发处理逻辑。
如果把报告状态分为初步报告和最终报告。在接收到初步报告后，仍然可以接收状态变更消息，而如果接受到最终报告，则不再接收后续消息。后面进入的消息被忽略。
如果再加上其他消息：比如取消申请、计费、申请单中的某个项目取消、退费、检查异常终止通知……

可以看出，这样的ad-hoc解决方案随着约束的增多、消息的增多，而越发难以维护。

***
##Kopp等的方法：choreography-aware ESB
使用BPELgold（扩展BPEL）作为interaction描述方式。把reaction方式与choreography模型一起形成extended choreography model。

* 时间约束：wait activity、pick的onAlarm分支和event handler
* correlation：使用correlationSet
* event gateway实现为pick块中的onInteraction。

把BPELgold转换为可执行的BPEL流程：interaction实现为pick中的onMessage-invoke，逻辑约束仍然沿用BPEL中的元素，比如sequence、wait等。
choreography-aware ESB实现：servicemix + apache ODE engine（进行了一些扩展）：

- 首先对进入的消息使用一个choreography manager判断是否存在一个对应的choreography（如何做？）
- 然后使用扩展ODE的ODEgold判断当前是否有一个接收此消息的receive activity（如何做？）

对于投资时间约束的例子来说，有一个问题是如何分辨描述acceptance和reject回复的两种消息，是定义就设计为不同的消息类型吗？
如果在BPEL中实现，就需要customer根据自己的选择去调用两个不同的operation。

采用此方法实现放射申请单（BPEL流程）：

    <pick: order request-invoke（获取response：receive-reply）/>
    <if：response是接收><branch><sequence>
        <pick: study complete-invoke>
        <pick: report-invoke>
    </sequence></branch><if>

此BPEL流程在BPEL引擎中运行，如果report提前到，则没有接收此种类型的activity，此时对这样的消息需要指定处理逻辑。
*其实每个pick块（onMessage-invoke）实际上都是一个集成channel。只不过adapter这种工作在路由至ODE引擎之前就做完了。*

**问题**

- 这个方法的缺点是需要针对每个receive、invoke实现web service接口，因为BPEL是基于web service的。
- 另外一个缺点是缺乏图形化能力。
- 如果考虑技术环节，重试是通过BPEL引擎的内置机制完成的，只有完成了对receiver的invoke，后面的元素如wait才能进行。不过，如果考虑非web service情况，当非标准接口采用web service进行封装时，BPEL的invoke只是把消息发送到了适配器提供的服务上，而实际receiver未必接收，可能需要多次重试。
这样的情况，在投资的例子中，比如invoke在t1时间把消息送给一个封装的服务，并且激活了wait节点，而实际上经过多次重试后在t2时间才把这个消息实际发送给客户。BPEL中的时间窗成为{t1, t1+t}，而实际上应为{t2, t2+t}。在时间窗的偏差中传入的客户选择消息会被误处理。
