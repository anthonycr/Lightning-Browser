Change Log
==========

Version 5.0.2 *(2019-09-07)*
----------------------------
- Target Android 10 API 29
- Update desktop and mobile user agents
- Fixed segfault that occurred when some URLs were clicked
- Added dialog to show SSL certificate info when SSL icon is tapped

Version 5.0.1 *(2019-09-01)*
----------------------------
- 64 bit I2P support

Version 5.0.0 *(2019-08-31)*
----------------------------
- Minimum SDK Version 19
- Target Android 28
- Converted project to Kotlin
- New icon
- Fixed bugs
    - Deleting an item from history now reloads the page
    - Downloads are now ordered by the time they were downloaded
    - Typing spaces in search suggestions no longer causes problems
    - Playing a video in full screen will now default to landscape orientation
    - Adding a shortcut now works on Android O
    - Search Suggestions no longer allowed in incognito for security
    - Pasting text into the search bar no longer keeps text styling
- Ad blocking
    - Added support for allowing/disallowing ads on per site basis
    - Ad block support on Lite version
    - Rewrote ad blocker to support new sources (only available on Plus version)
        - Default Hosts file
        - Hosts from URL
        - Hosts from local file
    - Improved ad blocker loading performance
- UI Improvements
    - Improved button backgrounds and enable/disable buttons when actions are disabled
    - Improved start up performance
    - Adding a bookmark now shows a dialog prompt to allow folder selection
    - Support re-opening tabs via long press on new tab in incognito
    - History is now opened in a new tab
    - Adding a bookmark now shows a toast
    - Added ongoing incognito notification to notify you there is an incognito window open
- Updated translations
- Browser now handles web search intents
- Ignoring an SSL warning now ignores it for the duration of the session
- Option to send 'save-data' header with requests
- Support for Naver search engine
- Support for WebRTC (Added permissions, camera + mic, which are asked for at runtime only when WebRTC is enabled) - by default disabled

Version 4.5.1 *(2017-06-28)*
----------------------------
- Fixed bug with folders disappearing on bookmark homepage
- Updated history page
- Updated bookmark page
- Updating target to Android O
- Updating default bookmark favicons
- Fixed occasional bug with bookmark long press
- Updated downloads page design
- Enhanced keyboard shortcuts
- Fixed bug in google search suggestions for certain languages

Version 4.5.0 *(2017-06-08)*
----------------------------
- Translation updates
- Memory leak fixes
- Basic keyboard shortcut support
- Improved browser dialogs
- Improved ad block efficiency
- Improved bookmark storage
- Added setting for black status bar
- Added downloads page
- Baidu search suggestions option
- Updated bookmark page UI
- Updated history page UI
- Fixed numerous bugs

Version 4.4.1 *(2016-11-05)*
----------------------------
- Fixed bookmark/tab drawer crash
- Fixed search suggestions crash on start up
- Fixed bug where links wouldn't open in the browser if you had the "restore lost tabs" option disabled

Version 4.4.0 *(2016-10-30)*
----------------------------
- Android 7.0 support
- Improved long-press menu
- Bookmark and tab drawers can be swapped
- Improved downloading of files
- Improved link handling for apps
- DuckDuckGo powered search suggestions
- UI improvements for readability
- Fixed text selection bugs in URL bar
- Fixed find in page bug
- Fixed image sync problems with bookmarks
- Fixed various crashes
- Fixed search suggestion bug


Version 4.3.3 *(2016-04-23)*
----------------------------
- Added option to request "Do Not Track"
- Added option to hide X-Requested-With header
- Added option to hide X-Wap-Profile
- Added language support in google search suggestions
- Added home screen widget
- Added new tab button on tablet UI
- Added tab add/remove animation on both tablet and phone UI
- Added close tabs dialog (long-press back button)
- Number icon instead of hamburger icon for better usability
- Back/forward arrows change color in tablet UI to indicate that you can go back/forward
- Search bar color now matches rest of UI
- Smoothed drawer/open close experience
- Fixed intent handling vulnerability
- Fixed downloading crash (marshmallow)
- Fixed various bugs related to settings, settings not getting remembered, etc.
- Fixed bugs that happened when browser went to the background
- Fixed bugs with search bar size changing/not changing when supposed to
- Updated ad block definition
- Started to improve app architecture

Version 4.2.3 *(2015-09-30)*
----------------------------
- Tablet UI (optional)
- Support for Marshmallow
- Improved Reading Mode
- Added animations between screens
- Fixed some memory leaks
- Added bookmark folder support!
- Improved full-screen mode
- Settings revamp (thanks community)
- Updated icons + UI cleanup
- Added actions to the bookmarks drawer
