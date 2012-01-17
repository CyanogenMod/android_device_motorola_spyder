# Copyright (C) 2011 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Input Device Calibration File for the Tuna touch screen.
#

# Basic Parameters
 touch.deviceType = touchScreen
touch.orientationAware = 1

# Size
touch.size.calibration = diameter
touch.size.scale = 10
touch.size.bias = 0
touch.size.isSummed = 0

# Tool Size
# Driver reports tool size as a linear width measurement summed over
# all contact points.
#
# Raw width field measures approx. 1 unit per millimeter
# of tool size on the surface where a raw width of 1 corresponds
# to about 17mm of physical size.  Given that the display resolution
# is 10px per mm we obtain a scale factor of 10 pixels / unit and
# a bias of 160 pixels.  In addition, the raw width represents a
# sum of all contact area so we note this fact in the calibration.
touch.toolSize.calibration = linear
touch.toolSize.linearScale = 10
touch.toolSize.linearBias = 160
touch.toolSize.isSummed = 1

# Pressure
# Driver reports signal strength as pressure.
#
# A normal thumb touch typically registers about 200 signal strength
# units although we don't expect these values to be accurate.
touch.pressure.calibration = amplitude
touch.pressure.source = default
touch.pressure.scale = 0.01

# Orientation
touch.orientation.calibration = none
