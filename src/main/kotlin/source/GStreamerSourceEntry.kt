package source

import edu.wpi.first.shuffleboard.api.sources.SourceEntry

class GStreamerSourceEntry(private val name: String) : SourceEntry {
    override fun getName(): String = "/$name"
    override fun getViewName(): String = name
    override fun getValueView(): Any? = null
    override fun getValue(): String = name
    override fun get(): GStreamerSource = GStreamerSourceType.forName(name)
}
