package vladsaif.syncedit.plugin.diffview

import vladsaif.syncedit.plugin.Binding

class BindingsProvider(
    @Volatile
    var bindings: List<Binding> = listOf()
)