package org.hexworks.zircon.internal.component.impl

import org.hexworks.cobalt.databinding.api.extensions.onChange
import org.hexworks.cobalt.datatypes.Maybe
import org.hexworks.zircon.api.behavior.TextHolder
import org.hexworks.zircon.api.builder.component.ComponentStyleSetBuilder
import org.hexworks.zircon.api.builder.graphics.StyleSetBuilder
import org.hexworks.zircon.api.color.TileColor
import org.hexworks.zircon.api.component.ColorTheme
import org.hexworks.zircon.api.component.ComponentStyleSet
import org.hexworks.zircon.api.component.Header
import org.hexworks.zircon.api.component.data.ComponentMetadata
import org.hexworks.zircon.api.component.renderer.ComponentRenderingStrategy
import org.hexworks.zircon.api.input.Input
import org.hexworks.zircon.internal.behavior.DefaultTextHolder

class DefaultHeader(componentMetadata: ComponentMetadata,
                    initialText: String,
                    private val renderingStrategy: ComponentRenderingStrategy<Header>)
    : Header, DefaultComponent(
        componentMetadata = componentMetadata,
        renderer = renderingStrategy), TextHolder by DefaultTextHolder(initialText) {

    init {
        render()
        textProperty.onChange {
            render()
        }
    }

    override fun acceptsFocus() = false

    override fun giveFocus(input: Maybe<Input>) = false

    override fun takeFocus(input: Maybe<Input>) {}

    override fun applyColorTheme(colorTheme: ColorTheme): ComponentStyleSet {
        return ComponentStyleSetBuilder.newBuilder()
                .withDefaultStyle(StyleSetBuilder.newBuilder()
                        .withForegroundColor(colorTheme.primaryForegroundColor)
                        .withBackgroundColor(TileColor.transparent())
                        .build())
                .build().also {
                    componentStyleSet = it
                    render()
                }
    }

    override fun render() {
        renderingStrategy.render(this, graphics)
    }
}
