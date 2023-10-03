# 插件化出现问题
desktop插件化方案时，遇见的问题！\
1. 该如何去实现在desktop下的热加载方案: \
目前想的方案看是否能抽取Android源码中的DexFile文件，自己去生成一个classloader去加载，然后通过该classload去生成对应的类

2. 以前的想法是构建一个apk后，通过不同的compose控件来控制最终desktop与App端