# Processor mechanism

这篇文档用于介绍jing项目中使用到的annotation processor机制

## Why use annotation processor

在jing项目中，需要解决诸如动态库API加载，序列化库等问题，通常对于这类问题，有以下几类解决方案：

1. 在compile-time解决，用annotation-processor做编译期的增强
2. 在build-time解决，借助构建工具来解决问题
3. 在run-time解决，通过反射等机制来解决问题

这几个解决方案各有优劣：

在compile-time用annotation processor可以根据已有的代码结构， 做额外的源代码生成或字节码生成，
实现一些手写的话会非常繁琐的逻辑，可以大幅度的简化开发，但是在编译期只能做新代码或字节码的生成，
并不能修改已有的代码，这些生成出来的逻辑还是要到运行时去加载后运行的，这对可实现的功能产生了一定的限制。

在build-time做修改可以大幅度的对已有项目进行修改，不过这需要和构建工具进行较为深度的整合，
要花费显著更多的时间和精力才能达到想要的效果。

在run-time通过反射去实现相关的逻辑可能是最简单实用的方式，用classfile提供的API去做运行时的字节码生成可以达成和compile-time
同等性能的效果，但这种方式对于用户而言不够透明，有太多的魔法在里面。

考虑到与project jigsaw模块化，以及和graalvm native image的集成，jing项目需要尽可能的维护模块间良好的依赖关系，且满足close-world assumption
因此我们决定最终使用annotation processor的方式来实现项目中，有关代码生成，元信息获取的部分。

## How annotation processor got used

在java规范中，RetentionPolicy有SOURCE，CLASS，RUNTIME三种，jing项目只使用到了SOURCE级别的注解，也就是，我们只会在源码级别处理注解

当注解会被应用于类上，annotation processor会扫描该类的代码结构，生成出所需的源代码，这些被生成的源码可以被用户在编译后自由的查看，以了解其行为
也可以在被Debug时应用断点，查看其运行时的具体变量值

对于生成的源码，在运行时使用到它们的方式有些特殊，我们不想要破坏任何用户在module-info中声明的依赖关系，因此我们使用了一些小小的HACK来规避该风险

在project jigsaw中，模块之间的依赖关系会在module-info中被定义，通常，开发者编写的业务代码会依赖于jing项目的代码，这种依赖是单向的

但在考虑到序列化等场景时，jing项目需要知道用户编写的类中包含哪些域，才能对其进行序列化操作，这就产生了循环依赖的问题

在java中，我们通常会用反射的方式来规避该问题，但是project jigsaw也对这种机制提出了明确的限制，只有用户显式open的模块才能被反射所访问，这会让开发者在编写module-info时困难重重

在jing项目中，我们利用了一个JVM的特性，使得开发者不需要主动对jing项目暴露接口，即能够让序列化等功能自动的获取到所需的信息：模块之间的相互依赖只会限制互相访问的情况，而类加载并不属于该范畴之内

也就是说，如果我们在用户编写的A模块中，生成了类B，在jing项目的源码中，可以顺利的手动触发类B的初始化过程，但是并不能访问或调用类B中的方法

在jing项目中，对于annotation processor的使用通常包含以下的几个流程：

1. 通过annotation processor，在编译期生成新的源代码文件
2. 当所有的annotation processor均处理完毕后，将生成类的类名，写入到resources文件夹中
3. 程序在运行时，读取resources中的相关文件，完成生成类的类加载流程
4. 在生成类的类加载流程中，会将所需数据传递给jing项目，从而在没有循环依赖的情况下做到了数据的双向传递


