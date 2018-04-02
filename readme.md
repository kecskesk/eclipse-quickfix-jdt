All plugins should be tested on this repo: 

https://github.com/kecskesk/java-plugin-v1

Usage: 
* use 1.8.M6 Photon! Jdk9.0.4 did not compile with Oxygen 4.7.3
* comment out needed plugin in
```
custom-markers/src/hu/kecskesk/custommarker/Activator.java
```
* select the menu item under refactorings

Todo: 

* create a switch for the refactors 
* test activating all of them
* finish immutables (only work for arrays aslist)
* test code with jUnit