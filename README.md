#Lightning Browser [![Build Status](https://travis-ci.org/anthonycr/Lightning-Browser.svg?branch=master)](https://travis-ci.org/anthonycr/Lightning-Browser)
####Speed, Simplicity, Security
#![](ic_launcher_small.png)
####Download
* [Download APK from here](https://github.com/anthonycr/Lightning-Browser/releases)

* [Download from Google Play](https://play.google.com/store/apps/details?id=acr.browser.barebones)

####Master Branch
* [![Build Status](https://travis-ci.org/anthonycr/Lightning-Browser.svg?branch=master)](https://travis-ci.org/anthonycr/Lightning-Browser)

####Dev Branch
* [![Build Status](https://travis-ci.org/anthonycr/Lightning-Browser.svg?branch=dev)](https://travis-ci.org/anthonycr/Lightning-Browser)

####Features
* Bookmarks

* History

* Multiple search engines (Google, Bing, Yahoo, StartPage, DuckDuckGo, etc.)

* Incognito mode

* Flash support (prior to 4.4)

* Follows Google design guidelines

* Unique utilization of navigation drawer for tabs

* Google search suggestions

* Orbot Proxy support

####Permissions

* ````INTERNET````: For accessing the web

* ````WRITE_EXTERNAL_STORAGE````: For downloading files from the browser

* ````READ_EXTERNAL_STORAGE````: For downloading files from the browser

* ````ACCESS_FINE_LOCATION````: For sites like Google Maps, it is disabled by default in settings and displays a pop-up asking if a site may use your location when it is enabled

* ````READ_HISTORY_BOOKMARKS````: To synchronize history and bookmarks between the stock browser and Lightning

* ````WRITE_HISTORY_BOOKMARKS````: To synchronize history and bookmarks between the stock browser and Lightning

* ````ACCESS_NETWORK_STATE````: Required for the WebView to function by some OEM versions of WebKit

####The Code
* Please contribute code back if you can. The code isn't perfect.
* Please add translations/translation fixes as you see need

####Contributing
* Contributions are always welcome
* If you want a feature and can code, feel free to fork and add the change yourself and make a pull request
* PLEASE use the ````dev```` branch when contributing as the ````master```` branch is supposed to be for stable builds. I will not reject your pull request if you make it on master, but it will annoy me and make my life harder.

####Setting Up the Project
Due to the inclusion of the netcipher library for Orbot proxy support, importing the project will show you some errors. To fix this, first run the following git command in your project folder (NOTE: You need the git command installed to use this):
````
git submodule update --init --recursive
````
Once you run that command, the IDE should automatically import netcipher and a couple submodules in as separate projects. Than you need to set the netcipher library project as a libary of the browser project however your IDE makes you do that. Once those steps are done, the project should be all set up and ready to go. [Please read this tutorial for more information on git submodules](http://www.vogella.com/tutorials/Git/article.html#submodules)

####License
````
Copyright 2014 Anthony Restaino

Lightning Browser

   This Source Code Form is subject to the terms of the 
   Mozilla Public License, v. 2.0. If a copy of the MPL 
   was not distributed with this file, You can obtain one at 
   
   http://mozilla.org/MPL/2.0/
````
This means that you MUST provide attribution in your application to Lightning Browser for the use of this code. The way you can do this is to provide a separate screen in settings showing what open-source libraries and/or apps (this one) you used in your application. You must also open-source any files that you use from this repository and if you use any code at all from this repository, the file you put it in must be open-sourced according the the MPL 2.0 license. To put it simply, if you create a fork of this browser, your browser must be open-source, no exceptions. The only way to avoid open-sourcing a file is to completely write all the code yourself and to not use any code from Lightning. This is in order to provide a way for companies to utilize the code without making private server code public. For further explanation, please email me, or seek legal counsel :-P

If you have any questions regarding the open-source license, please contact me at [anthonyrestaino11@gmail.com](mailto:anthonyrestaino11@gmail.com)
