#
# Copyright (C) 2012 Texas Instruments Inc.
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

OMAP_ENHANCEMENT := true
#OMAP_ENHANCEMENT_BURST_CAPTURE := true
#OMAP_ENHANCEMENT_S3D := true
#OMAP_ENHANCEMENT_CPCAM := true
#OMAP_ENHANCEMENT_VTC := true

define ti-clear-vars
$(eval OMAP_ENHANCEMENT:=) \
$(eval OMAP_ENHANCEMENT_BURST_CAPTURE:=) \
$(eval OMAP_ENHANCEMENT_S3D:=) \
$(eval OMAP_ENHANCEMENT_CPCAM:=) \
$(eval OMAP_ENHANCEMENT_VTC:=)
endef

