# QuickBle
QuickBle is an android ble extension that makes BLE operations easy and quick. 

QuickBle use Jasonchenlijian's [FastBle](https://github.com/Jasonchenlijian/FastBle) as the BLE operation lib. 

With QuickBle, you can make several BLE requests in a time, 
the QuickBle will enqueue every request to a queue and executed it sequentially.
And the response will return in a BleCallback that you can do something with 
the result.


# Usage
1. First you need to add gradle dependency in your project:

```groovy
dependencies {
    ......
    compile 'kevinho.lib.dev:quickble:0.1.1'
}
```

2. Init the QuickBle instance with a Config object in your Application's onCreate() method:
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

3. Then you can do some BLE requests with `QuickBle.handler()` in your activity:
```java
// characteristic read operation 
QuickBle.handler().requestCharacteristicRead(..);
  
// characteristic write operation 
QuickBle.handler().requestCharacteristicWrite(...);
  
// characteristic notify operation 
QuickBle.handler().requestCharacteristicNotification(...);
```
method signature can see on the doc. 

4. If you want to do something with the BLE response result you can register the `BleCallback`:
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

