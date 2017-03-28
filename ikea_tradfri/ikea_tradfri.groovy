metadata {
    definition (name: "IKEA Trådfri", namespace: "edvaldeysteinsson", author: "Edvald Eysteinsson") {

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
        command "setColorWarmGlow"
        command "setColorWarmWhite"
        command "setColorCoolWhite"
        command "setColorTemperatureAndLevel"
        command "setNiceLevel"
        
        fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden",  model: "TRADFRI bulb E27 WS - opal 980lm", deviceJoinName: "TRADFRI bulb E27 WS - opal 980lm"
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
                attributeState "level", action:"setNiceLevel"
            }
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 4, height: 1, inactiveLabel: false, range:"(2200..4000)") {
            state "colorTemperature", action:"color temperature.setColorTemperature"
        }
        
        valueTile("colorName", "device.colorName", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
            state "colorName", label: '${currentValue}'
        }
        
        standardTile("colorWarmGlow", "device.default", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Warm Glow", action:"setColorWarmGlow", icon:"https://github.com/edvaldeysteinsson/SmartThingsResources/blob/master/ikea_tradfri/warm_glow.png?raw=true"
        }
        
        standardTile("colorWarmWhite", "device.default", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Warm White", action:"setColorWarmWhite", icon:"https://github.com/edvaldeysteinsson/SmartThingsResources/blob/master/ikea_tradfri/warm_white.png?raw=true"
        }
        
        standardTile("colorCoolWhite", "device.default", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Cool White", action:"setColorCoolWhite", icon:"https://github.com/edvaldeysteinsson/SmartThingsResources/blob/master/ikea_tradfri/cool_white.png?raw=true"
        }

        main(["switch"])
        details(["switch", "colorTempSliderControl", "refresh", "colorName", "colorWarmGlow", "colorWarmWhite", "colorCoolWhite"])
    }
}

// parse events into attributes
def parse_new(description) {
    log.debug "parse() - $description"
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
    log.debug "description is $description"
    def event = zigbee.getEvent(description)
    if (event) {
        if (event.name=="level" && event.value==0) {}
        else {
            if (event.name=="colorTemperature") {
                setGenericName(event.value)
            }
            sendEvent(event)
        }
    }
    else {
        def cluster = zigbee.parse(description)

        if (cluster && cluster.clusterId == 0x0006 && cluster.command == 0x07) {
            if (cluster.data[0] == 0x00) {
                log.debug "ON/OFF REPORTING CONFIG RESPONSE: " + cluster
                sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
            }
            else {
                log.warn "ON/OFF REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
            }
        }
        else {
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

def setNiceLevel(value) {
    // this will set the color temperature based on the level, 2200(0%) to 2700(100%)
    // it's a bit more like how a traditional filament bulb behaves
    zigbee.setLevel(value) + zigbee.setColorTemperature(2200 + (5*value))
}

def setLevel(value) {
    zigbee.setLevel(value)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    return zigbee.onOffRefresh()
}

def refresh() {
	zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.colorTemperatureRefresh() + zigbee.onOffConfig(0, 300) + zigbee.levelConfig() + zigbee.colorTemperatureConfig()
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    // Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
    // enrolls with default periodic reporting until newer 5 min interval is confirmed
    sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

    // OnOff minReportTime 0 seconds, maxReportTime 5 min. Reporting interval if no activity
    refresh()
}

def setColorWarmGlow() {
    setColorTemperature(2200)
}

def setColorWarmWhite() {
    setColorTemperature(2700)
}

def setColorCoolWhite() {
    setColorTemperature(4000)
}

def setColorTemperature(value) {
    setGenericName(value)
	zigbee.setColorTemperature(value)
}

def setColorTemperatureAndLevel(value) {
    setGenericName(value.temperature)
	zigbee.setLevel(value.level) + zigbee.setColorTemperature(value.temperature)
}

def setGenericName(value){
    if (value != null) {
        def genericName
        
        if (value < 2450) {
            genericName = "Warm Glow" // 2200 is named Warm Glow by IKEA so i use that for 2200-2449
        } else if (value < 2950) {
            genericName = "Warm White" // 2700 is named Warm White by IKEA so i use that for 2450-2949
        } else if (value <= 4000) {
            genericName = "Cool White" // 4000 is named Cool White by IKEA so i use that for 2950-4000
        }
        
        sendEvent(name: "colorName", value: genericName)
    }
}

def installed() {
    if ((device.currentState("level")?.value == null) || (device.currentState("level")?.value == 0)) {
        sendEvent(name: "level", value: 100)
    }
}
