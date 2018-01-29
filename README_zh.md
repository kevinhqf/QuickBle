# QuickBle
QuickBle 是一个Android BLE的扩展库，可以对BLE进行快速便捷的操作。 

QuickBle 使用了 Jasonchenlijian的[FastBle](https://github.com/Jasonchenlijian/FastBle) 作为BLE操作库. 

由于BLE操作是异步的，但每一次的操作请求都必须等待前一次的请求返回才能继续进行，这样导致了效率的下降。  

通过使用QuickBle,可以同一时间对BLE外设发起不同的多个GATT读写请求,而不需要等待前一个请求返回后再进行下一个请求。 
QuickBle 会将每一个请求放入到请求队列中，并且按顺序的从队列中取出请求进行处理。BLE操作响应的
回调结果同样也会通过回调返回给调用者，调用者只需进行`BleCallback`的监听注册即可。


# Usage
1. 首先将QuickBle依赖加入到你的`build.gradle`中：

```groovy
dependencies {
    ......
    compile 'kevinho.lib.dev:quickble:0.1.2'
}
```

2. 在AndroidManifest中添加蓝牙所需权限：
```xml
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.BLUETOOTH_PRIVILEGED"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

3. 在`Application`的`onCreate()`方法中对QuickBle进行初始化：
```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
       QuickBle.Config config =  new QuickBle.Config(this)
                        .isFilter(true)
                        .maxConnection(7)
                        .timeout(5000);
        QuickBle.instance().init(config);
    }
}
```

4. 通过`QuickBle.handler()`进行BLE的操作:
```java
// characteristic read operation 
QuickBle.handler().requestCharacteristicRead(..);
  
// characteristic write operation 
QuickBle.handler().requestCharacteristicWrite(...);
  
// characteristic notify operation 
QuickBle.handler().requestCharacteristicNotification(...);
```


5.注册`BleCallback`对BLE请求操作的回调数据进行处理:
```java
    BleCallback mBleCallback = new BleCallback() {...};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ....
        QuickBle.registerCallback(mBleCallback);
    }
    
    
     @Override
     protected void onDestroy() {
        super.onDestroy();
        QuickBle.unregisterCallback(mBleCallback);
     }
    
```

# License
```
   Copyright 2018 kevinhqf

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

