package sync.ui

import grails.validation.Validateable

class ScheduleEntry implements Validateable {
    static String prefix = "schedule/"

    static List<ScheduleEntry> list(ConfigService configService) {
        configService.listConfigObjects(prefix).collect {
            new ScheduleEntry([configService: configService, xmlKey: it])
        }.sort { a, b -> a.name <=> b.name }
    }

    ConfigService configService

    String name
    @Lazy
    String xmlKey = "${prefix}${name}.xml"
    @Lazy
    allKeys = [xmlKey]
    @Lazy(soft = true)
    ScheduledSync scheduledSync = exists() ? configService.readConfigObject(xmlKey, ScheduledSync.class) : new ScheduledSync()

    boolean exists() {
        return (name && configService && configService.configObjectExists(xmlKey))
    }

    def write() {
        configService.writeConfigObject(xmlKey, scheduledSync, 'application/xml')
    }

    def setXmlKey(String key) {
        this.name = key.split('/').last().replaceFirst(/[.]xml$/, '')
    }

    static constraints = {
        configService nullable: true
        name blank: false
        // TODO: use @Option annotations to dynamically validate what we can before running the sync
        //scheduledSync validator: { it.validate() ?: 'invalid' }
    }
}
