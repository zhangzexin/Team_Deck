Team Deck App
### 
用于通过手机与电脑交互，实现通过手机实现类似直播游戏中控

### 目录
#### desktopApp
windows端应用逻辑及其资源文件

#### androidApp
安卓端App代码

#### 实现原理
通过Compose插件化来进行实现，App通过websocket通知desktop端触发事件，执行对应脚本，理论来说可以是Python,C++,Java等,需要自己实现对应的脚本环境

#### 插件部署
通过插件化生成对应的apk文件，文件中包含desktop端item项Ui实现，App端item项Ui实现，数据交互进行隐藏，考虑可以通过反射或者注解，实现item项的数据传递，不需要关注item所在的位置，当然apk中还会包含对应脚本。
