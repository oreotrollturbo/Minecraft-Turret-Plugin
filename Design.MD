# General design patterns


### Class and variable names
For Class,variable and function names I use camelCase.  
Classes start with uppercase whereas
variables and functions start with lowercase letters . 

```kotlin
ExampleClass.kt
fun exampleFunction(){}
var exampleVariable;
```

### Conditional statements / checks

For any conditional statements I use guard clauses to avoid excessive nesting.  
So instead of the code looking like this :
```kotlin
if (act == Action.RIGHT_CLICK_BLOCK) { 
    player.sendMessage("Right clicked")
}
```
It looks like this 
```kotlin
if (act != Action.RIGHT_CLICK_BLOCK) { 
    return
}

player.sendMessage("Right clicked")
```
This makes the code much cleaner and easier to maintain


### Comments
For short explanations of what a specific variable, if statement or line of code in general I use // comments.  
  
If the explanation for something is longer or more important (mainly when it comes to functions) I use
/** **/  comments 


# "Device" class patters
- Each device is represented as its own class, and all relevant information,
such as health and armor stands, is stored within that class.


- All logic for a device should be encapsulated within methods of the object and should only be accessed by calling
those methods through the object instance. Avoid manually editing any values directly
```kotlin
turret.damage(10)

//instead of 

turret.health -= 10
```
- Logic that relates to a device but doesn't depend on a specific instance is kept in a companion object at the bottom of the class

