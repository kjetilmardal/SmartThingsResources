/**
 *  This is free and unencumbered software released into the public domain.
 *  
 *  Anyone is free to copy, modify, publish, use, compile, sell, or
 *  distribute this software, either in source code form or as a compiled
 *  binary, for any purpose, commercial or non-commercial, and by any
 *  means.
 *  
 *  In jurisdictions that recognize copyright laws, the author or authors
 *  of this software dedicate any and all copyright interest in the
 *  software to the public domain. We make this dedication for the benefit
 *  of the public at large and to the detriment of our heirs and
 *  successors. We intend this dedication to be an overt act of
 *  relinquishment in perpetuity of all present and future rights to this
 *  software under copyright law.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 *  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 *  
 *  For more information, please refer to <http://unlicense.org/>
 */
metadata {
    definition (name: "IKEA Trådfri-2", namespace: "edvaldeysteinsson", author: "Edvald Eysteinsson") {

        capability "Actuator"
        capability "Color Temperature"
        capability "Configuration"
        capability "Health Check"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"
        capability "Light"

        attribute "colorName", "string"
        
        command "setGenericName"
        command "setColorRelax"
        command "setColorEveryday"
        command "setColorFocus"
	
    	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden",  model: "TRADFRI bulb E27 WS opal 1000lm", deviceJoinName: "TRADFRI bulb E27 WS opal 1000lm"
    	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden",  model: "TRADFRI bulb E27 WS�opal 980lm", deviceJoinName: "TRADFRI bulb E27 WS opal 980lm"
    	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden",  model: "TRADFRI bulb E27 WS clear 950lm", deviceJoinName: "TRADFRI bulb E27 WS clear 950lm"
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden",  model: "TRADFRI bulb E14 WS opal 400lm", deviceJoinName: "TRADFRI bulb E14 WS opal 400lm"
    	fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden",  model: "TRADFRI bulb GU10 WS 400lm", deviceJoinName: "TRADFRI bulb GU10 WS 400lm"
    }

    // UI tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.light.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"setLevel"
            }
        }

        controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 4, height: 2, inactiveLabel: false, range:"(2200..4000)") {
            state "colorTemperature", action:"color temperature.setColorTemperature"
        }
        
        valueTile("colorName", "device.colorName", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "colorName", label: '${currentValue}'
        }
        
        standardTile("colorRelax", "device.default", inactiveLabel: false, width: 2, height: 2) {
            state "default", label:"", action:"setColorRelax", backgroundColor:"#ECCF73"
        }
        
        standardTile("colorEveryday", "device.default", inactiveLabel: false, width: 2, height: 2) {
            state "default", label:"", action:"setColorEveryday", backgroundColor:"#FBECCB"
        }
        
        standardTile("colorFocus", "device.default", inactiveLabel: false, width: 2, height: 2) {
            state "default", label:"", action:"setColorFocus", backgroundColor:"#F5FBFB"
        }

        main(["switch"])
        details(["switch", "colorTempSliderControl", "colorName", "colorRelax", "colorEveryday", "colorFocus"])
    }
}

// parse events into attributes
def parse_new(description) {
    def results = []

    def map = description
    if (description instanceof String)  {
        map = stringToMap(description)
    }

    if (map?.name && map?.value) {
        results << createEvent(name: "${map?.name}", value: "${map?.value}")
    }
    results
}

// Parse incoming device messages to generate events
def parse(String description) {
    def event = zigbee.getEvent(description)

    if (event) {
        if (event.name=="level" && event.value==0) {}
        else {
            if (event.name=="colorTemperature") {
                setGenericName(event.value)
            }
            sendEvent(event)
        }
    } else {
        def cluster = zigbee.parse(description)

        if (cluster && cluster.clusterId == 0x0006 && cluster.command == 0x07) {
            if (cluster.data[0] == 0x00) {
                sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
            } else {
                log.warn "ON/OFF REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
            }
        } else {
            log.warn "DID NOT PARSE MESSAGE for description : $description"
            log.debug "${cluster}"
        }
    }
}

def off() {
    zigbee.off()
}

def on() {
    zigbee.on()
}

def setLevel(value) {
    // this will set the color temperature based on the level, 2200(0%) to 2700(100%)
    // it's a bit more like how a traditional filament bulb behaves
    zigbee.setLevel(value) + zigbee.setColorTemperature(2200 + (5*value))
}

def setColorRelax() {
    setColorTemperature(2200)
}

def setColorEveryday() {
    setColorTemperature(2700)
}

def setColorFocus() {
    setColorTemperature(4000)
}

def setColorTemperature(value) {
    setGenericName(value)
	zigbee.setColorTemperature(value)
}

def setGenericName(value){
    if (value != null) {
        def genericName
        
        if (value < 2450) {
            genericName = "Relax" // 2200 is named Relax by IKEA so i use that for 2200-2449
        } else if (value < 2950) {
            genericName = "Everyday" // 2700 is named Everyday by IKEA so i use that for 2450-2949
        } else if (value <= 4000) {
            genericName = "Focus" // 4000 is named Focus by IKEA so i use that for 2950-4000
        }
        
        sendEvent(name: "colorName", value: genericName)
    }
}

def installed() {
    if ((device.currentState("level")?.value == null) || (device.currentState("level")?.value == 0)) {
        sendEvent(name: "level", value: 100)
    }
}
