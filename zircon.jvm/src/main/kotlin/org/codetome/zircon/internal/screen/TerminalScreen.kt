package org.codetome.zircon.internal.screen

import org.codetome.zircon.api.Position
import org.codetome.zircon.api.builder.ComponentStyleSetBuilder
import org.codetome.zircon.api.screen.Screen
import org.codetome.zircon.api.terminal.Terminal
import org.codetome.zircon.internal.component.InternalContainerHandler
import org.codetome.zircon.internal.component.impl.DefaultContainer
import org.codetome.zircon.internal.component.impl.DefaultContainerHandler
import org.codetome.zircon.internal.event.EventBus
import org.codetome.zircon.internal.event.Event
import org.codetome.zircon.internal.terminal.InternalTerminal
import org.codetome.zircon.internal.terminal.virtual.VirtualTerminal
import org.codetome.zircon.internal.util.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class implements the logic defined in the [Screen] interface.
 * A [TerminalScreen] wraps a [Terminal] and uses a [VirtualTerminal] as a backend
 * for its changes. When `refresh` or `display` is called the changes are written to
 * the [Terminal] this [TerminalScreen] wraps. This means that a [TerminalScreen] acts
 * as a double buffer for the wrapped [Terminal].
 */
class TerminalScreen(private val terminal: InternalTerminal,
                     private val backend: VirtualTerminal = VirtualTerminal(
                             initialSize = terminal.getBoundableSize(),
                             initialFont = terminal.getCurrentFont()),
                     private val containerHandler: InternalContainerHandler = DefaultContainerHandler(DefaultContainer(
                             initialSize = terminal.getBoundableSize(),
                             position = Position.defaultPosition(),
                             componentStyleSet = ComponentStyleSetBuilder.DEFAULT,
                             initialFont = terminal.getCurrentFont())))
    : InternalScreen,
        InternalTerminal by backend,
        InternalContainerHandler by containerHandler {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val id = Identifier.randomIdentifier()

    init {
        EventBus.subscribe<Event.ScreenSwitch> { (screenId) ->
            if (id != screenId) {
                logger.info("Deactivating screen due to screen switch...")
                deactivate()
            }
        }
        EventBus.subscribe<Event.ComponentChange> {
            if (isActive()) {
                refresh()
            }
        }
        EventBus.subscribe<Event.RequestCursorAt> { (position) ->
            if (isActive()) {
                terminal.setCursorVisibility(true)
                terminal.putCursorAt(position)
            }
        }
        EventBus.subscribe<Event.HideCursor> {
            if (isActive()) {
                terminal.setCursorVisibility(false)
            }
        }
    }

    override fun getId() = id

    @Synchronized
    override fun display() {
        EventBus.broadcast(Event.ScreenSwitch(id))
        setCursorVisibility(false)
        putCursorAt(Position.defaultPosition())
        flipBuffers(true)
        activate()
    }

    @Synchronized
    override fun refresh() {
        flipBuffers(false)
    }

    private fun flipBuffers(forceRedraw: Boolean) {
        val positions = if (forceRedraw) {
            getBoundableSize().fetchPositions()
        } else {
            drainDirtyPositions()
        }
        positions.forEach { position ->
            val character = backend.getCharacterAt(position).get()
            terminal.setCharacterAt(position, character)
        }
        // TODO: optimize this
        terminal.drainLayers()
        transformComponentsToLayers().forEach {
            terminal.pushLayer(it)
        }
        backend.getLayers().forEach {
            terminal.pushLayer(it)
        }
        if (hasOverrideFont()) {
            terminal.useFont(getCurrentFont())
        }
        terminal.flush()
    }
}
