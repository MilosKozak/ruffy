# ruffy
Gum Gum to the pump!
[![Join the chat at https://gitter.im/monkey-r/Lobby](https://badges.gitter.im/monkey-r/Lobby.svg)](https://gitter.im/monkey-r/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

<img src="https://cdn.rawgit.com/monkey-r/ruffy/master/ruffy-logo.svg" align="right" width="200" alt="ruffy logo">
This is not even alpha Software!

USE IT AT YOUR OWN RISK!
I AM NOT RESPONSIBLE FOR ANY DAMAGES ON YOUR PHONE, PUMP, HEALTH OR WHAT EVER GETS DAMAGED IF YOU USE THIS SOFTWARE!
DON'T USE IT WHILE PUMP IS ATTACHED TO YOUR BODY!
USE IT AT YOUR OWN RISK!

Neither this project or me or anybody who has worked on this project is affiliated, or endorsed, by ROCHE or any of its national or international subsidiaries!

This work is presented as it is, no one should ever:

1. claim any rights about this software at any time
1. build products based on this software
1. release binary builds of this software
1. try to harm anybody with this software
    
This ongoing work is made available by a small, dedicated group of people who want to use the pump as a device in their personal artificial pancreas research.

What is ready:

* pair with a pump
* connect with a pump
* show the menu (as the meter would do)
* structurally parse the informations on screen (not all screens)
* remote control the pump via buttons

What are the requirements:

* Android below 4.2
* Lineage 7+ with latest nightly

What must I change to use another Android version?

* nothing, you just can not use another version!

Why?

* because broadcom implemented a really strange behaviour into the bluedroid stack which is used since Android 4.2, this we can not compensate!

But Why?

* Because you have not read or understood the other answers :(  

How do I build it?

* Download a 2.3 Version of Android-Studio
* Set it up with the wizard
* clone this project from github
* open it with android studio
* connect your (hopefully compatible) phone
* press the run button on top 

How do I build it directly from my device?

* Look at : http://www.android-ide.com/tutorial_androidstudio.html
* Download AIDE
* clone this project from github
* Build

How do I know that my bluetooth Stack is wrong?

* start the pairing, lay the pump next to the phone in pairing mode
* if the pump does not find your phone (shows it as selectable device in the screen) then it wont work!

Why the name?

* I watched One piece, the anime about the pirate Monkey D. Ruffy while starting the project :D  
