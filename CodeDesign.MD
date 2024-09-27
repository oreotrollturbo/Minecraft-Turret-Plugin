# General code/design patterns


### Class and variable names
For Class,variable and 
function names camelCase is used.  
Classes start with uppercase whereas
variables and functions start with lowercase letters . 

```kotlin
ExampleClass.kt
fun exampleFunction(){}
var exampleVariable
```

### Conditional statements / checks

For any conditional statements use guard clauses to avoid excessive nesting.  
So instead of the code looking like this :
```kotlin
if (act == Action.RIGHT_CLICK_BLOCK) { 
    player.sendMessage("Right clicked")
}
```
It looks like this  :
```kotlin
if (act != Action.RIGHT_CLICK_BLOCK) { 
    return
}

player.sendMessage("Right clicked")
```
This makes the code much cleaner and easier to maintain.

### "Utils" singleton
- This singleton is in charge of any misc functions that are used all over the plugin.
- Keep in mind that functions that have to do with a specific object should be kept within it in a companion object
for example the companion object in [Controller.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fobjects%2FController.kt) .

# Custom items
- All custom items are created and managed in the [ItemManager.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fitems%2FItemManager.kt) Object .
- No custom crafting recipes are made within the plugin it is assumed that the user will use Wolfy's custom crafting plugin
as stated in the [Readme.MD](Readme.MD) .
- Any checks that relate to custom items like checking if a player is holding a custom item,
an item stack is of a custom item etc. should be located in the [ItemManager.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fitems%2FItemManager.kt)
Object (preferably at the bottom).

# Data storage
Gson is used to store data in this project as JSON files
- All functions that interact with the file are stored within the main class [RCD_plugin.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2FRCD_plugin.kt) .
- Functions can be marked "public" if they need to be used within a different class .
- The only function that should be called on startup should be the loadSavedData() function

# "Device" classes
- Each device is represented as its own class for example [Turret.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fturrets%2FTurret.kt) and they inherit from [DeviceBase.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fobjects%2FDeviceBase.kt)
which stores data and logic that all Devices share .


- All logic for a device should be encapsulated within methods of the object and should only be accessed by calling
those methods through the object instance. Avoid directly editing any values .
```kotlin
turret.damage(10)

//instead of 

turret.health -= 10
```
- Logic that relates to a device but doesn't depend on a specific instance
is kept in a companion object at the bottom of the class like at the bottom of [Turret.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fturrets%2FTurret.kt)

# "Controller" class
- When a player begins controlling a device, a Controller object is instantiated to manage all the 
data and functions related to the player. 

- This class is also responsible for handling the villager that is spawned when the player enters control mode

- All controller instances are stored in the "controllingDevice" list in [RCD_plugin.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2FRCD_plugin.kt)
 and within the Device itself as a variable .


- Make sure the controller instance is removed from the list and variable within the device .

# Data classes
- [TurretConfigs.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fdata%2FTurretConfigs.kt) : A more compact way to get config 
values for the Turret object .
- [DroneConfigs.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fdata%2FDroneConfigs.kt) configs for the drone
- [TurretSaveData.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fdata%2FTurretSaveData.kt) : Holds all the data
that is needed to save a Turret instance (in the turrets.json file) upon server shutdown .