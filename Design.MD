# General code/design patterns


### Class and variable names
For Class,variable and  function names camelCase is used.  
Classes start with uppercase whereas
variables and functions start with lowercase letters . 

```kotlin
ExampleClass.kt
fun exampleFunction(){}
var exampleVariable;
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

# Custom item patterns
- All custom items are created and managed in the [ItemManager.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fitems%2FItemManager.kt) Object .
- No custom crafting recipes are made within the plugin it is assumed that the user will use Wolfy's custom crafting plugin
as stated in the [Readme.MD](Readme.MD) .
- Any checks that relate to custom items like checking if a player is holding a custom item,
an item stack is of a custom item etc. should be located in the [ItemManager.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fitems%2FItemManager.kt)
Object (preferably at the bottom).

# "Device" class patters
- Each device is represented as its own class for example [Turret.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fturrets%2FTurret.kt) 
, and all relevant information,
such as health, UUID and even armorStands is stored within that class.


- All logic for a device should be encapsulated within methods of the object and should only be accessed by calling
those methods through the object instance. Avoid manually editing any values directly .
```kotlin
turret.damage(10)

//instead of 

turret.health -= 10
```
- Logic that relates to a device but doesn't depend on a specific instance
is kept in a companion object at the bottom of the class like at the bottom of [Turret.kt](src%2Fmain%2Fjava%2Forg%2Foreo%2Frcdplugin%2Fturrets%2FTurret.kt)


