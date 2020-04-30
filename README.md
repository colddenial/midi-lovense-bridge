# Midi Lovense Bridge #

This tool is designed to make it easy to control your lovense toys with a midi controller.

Download page for packages:
https://openstatic.org/lovense/

### Why? ###
I've always been a fan of the MIDI protocol, its a great way to control and automate simple devices. This software translates the 0-127 value from control sliders and note messages to a simple 0-20 value for the target device. Part of my inspiration came from the movie "Barbarella" where Durand-Durand uses an organ of sorts to control his odd sex machine. The possibilities are endless, you can use just about any MIDI device from the Rock Band Pro guitar to a simple DJ control surface.

![](http://openstatic.org/lovense/bridge.png)

### How do I use it? ###
I tried to make usage really simple and straightforward. This software utilizes the LAN API provided by the "Lovense Connect" app. This app should automatically recognize your toy a few seconds after it's been paired with the Lovense Connect app on your phone. In order for everything to work your phone and computer must be on the same Wifi Network.

[https://play.google.com/store/apps/details?id=com.lovense.connect](https://play.google.com/store/apps/details?id=com.lovense.connect "Lovense Connect on Google Play")

[https://apps.apple.com/us/app/lovense-connect/id1273067916](https://apps.apple.com/us/app/lovense-connect/id1273067916 "Lovense Connect on iTunes Store")

Once your toy becomes visible from the "Midi Lovense Brige" all you need to do is plug in your MIDI controller. All available hardware MIDI controllers should be listed on the left hand side of the app. To connect to a device simply check it off.

**Manually Connect**
If you are having trouble finding your Lovense Connect click on file -> enter ip/port manually. You can then enter the ip address of lovense connect along with the https port.

**Creating a rule**
"Rules" are mappings from your MIDI controller to your lovense device. You can create a rule by double-clicking on the toy you wish to create a rule for.

![](http://openstatic.org/lovense/rule_editor.png)

- Select Command - This lets you choose between control change messages and Note On/Off Messages
- Select Channel - Choose which MIDI Channel to listen on
- Select CC# - Select which control change number to listen for
- Data Range - How to interpret the message data
  - FULL (0-127) - Map the output from 0-127 to the Toy
  - TOP (64-127) - Only the top half of the slider should control the toy (basically middle is off)
  - BOTTOM INV (63-0) - Only the bottom half of the slider should control the toy (basically middle is off)
  - FULL INV (0-127) - Map the output from 127-0 to the Toy (reverses direction of slider)
- Toy Output - Select how to control the toy
  - VIBRATE - Controls the toys Vibrate function
  - VIBRATE1 - Control Vibrator 1 (edge)
  - VIBRATE2 - Control Vibrator 2 (edge)
  - ROTATE - Control rotation (nora)
  - AIR - Control Air (max)

Once you've set all the parameters for your rule. Just click "Create Rule" you can go back and edit a rule at any time just by clicking on it (rules are listed at the bottom of the interface)

    Copyright (C) 2019  colddenial

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.