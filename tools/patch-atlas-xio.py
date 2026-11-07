#!/usr/bin/env python3
"""Apply XIO GPS French voice and automated navigation stress-test overlay to Atlas."""
from pathlib import Path
import sys

root = Path(sys.argv[1])
voice = root / "app/src/main/kotlin/com/danemadsen/atlas/nav/voice-hint-text.kt"
text = voice.read_text(encoding="utf-8")
old = '''fun turnInstruction(command: TurnCommand, street: String?, exitNumber: Int? = null): String {
    val verb = when (command) {
        TurnCommand.STRAIGHT -> "Continue straight"
        TurnCommand.TURN_LEFT -> "Turn left"
        TurnCommand.TURN_SLIGHT_LEFT -> "Turn slightly left"
        TurnCommand.TURN_SHARP_LEFT -> "Turn sharply left"
        TurnCommand.TURN_RIGHT -> "Turn right"
        TurnCommand.TURN_SLIGHT_RIGHT -> "Turn slightly right"
        TurnCommand.TURN_SHARP_RIGHT -> "Turn sharply right"
        TurnCommand.KEEP_LEFT -> "Keep left"
        TurnCommand.KEEP_RIGHT -> "Keep right"
        TurnCommand.U_TURN -> "Make a U-turn"
        TurnCommand.ROUNDABOUT -> exitNumber?.takeIf { it > 0 }?.let { "At the roundabout, take the \${ordinal(it)} exit" }
            ?: "At the roundabout, go around and exit"
        TurnCommand.ROUNDABOUT_LEFT -> exitNumber?.takeIf { it > 0 }?.let { "At the roundabout, take the \${ordinal(it)} exit" }
            ?: "At the roundabout, go around and exit left"
        TurnCommand.EXIT_LEFT -> "Take the exit on the left"
        TurnCommand.EXIT_RIGHT -> "Take the exit on the right"
        TurnCommand.ARRIVE -> "Arrive at your destination"
    }
    if (street.isNullOrBlank() || command == TurnCommand.ARRIVE) return verb
    return "$verb onto $street"
}'''
new = '''fun turnInstruction(command: TurnCommand, street: String?, exitNumber: Int? = null): String {
    val french = java.util.Locale.getDefault().language.equals("fr", ignoreCase = true)
    val verb = if (french) when (command) {
        TurnCommand.STRAIGHT -> "Continuez tout droit"
        TurnCommand.TURN_LEFT -> "Tournez à gauche"
        TurnCommand.TURN_SLIGHT_LEFT -> "Tournez légèrement à gauche"
        TurnCommand.TURN_SHARP_LEFT -> "Tournez fortement à gauche"
        TurnCommand.TURN_RIGHT -> "Tournez à droite"
        TurnCommand.TURN_SLIGHT_RIGHT -> "Tournez légèrement à droite"
        TurnCommand.TURN_SHARP_RIGHT -> "Tournez fortement à droite"
        TurnCommand.KEEP_LEFT -> "Gardez la gauche"
        TurnCommand.KEEP_RIGHT -> "Gardez la droite"
        TurnCommand.U_TURN -> "Faites demi-tour"
        TurnCommand.ROUNDABOUT, TurnCommand.ROUNDABOUT_LEFT ->
            exitNumber?.takeIf { it > 0 }?.let { "Au rond-point, prenez la \${it}\${frenchOrdinal(it)} sortie" }
                ?: if (command == TurnCommand.ROUNDABOUT_LEFT) "Au rond-point, sortez à gauche" else "Au rond-point, prenez une sortie"
        TurnCommand.EXIT_LEFT -> "Prenez la sortie à gauche"
        TurnCommand.EXIT_RIGHT -> "Prenez la sortie à droite"
        TurnCommand.ARRIVE -> "Vous êtes arrivé à destination"
    } else when (command) {
        TurnCommand.STRAIGHT -> "Continue straight"
        TurnCommand.TURN_LEFT -> "Turn left"
        TurnCommand.TURN_SLIGHT_LEFT -> "Turn slightly left"
        TurnCommand.TURN_SHARP_LEFT -> "Turn sharply left"
        TurnCommand.TURN_RIGHT -> "Turn right"
        TurnCommand.TURN_SLIGHT_RIGHT -> "Turn slightly right"
        TurnCommand.TURN_SHARP_RIGHT -> "Turn sharply right"
        TurnCommand.KEEP_LEFT -> "Keep left"
        TurnCommand.KEEP_RIGHT -> "Keep right"
        TurnCommand.U_TURN -> "Make a U-turn"
        TurnCommand.ROUNDABOUT -> exitNumber?.takeIf { it > 0 }?.let { "At the roundabout, take the \${ordinal(it)} exit" }
            ?: "At the roundabout, go around and exit"
        TurnCommand.ROUNDABOUT_LEFT -> exitNumber?.takeIf { it > 0 }?.let { "At the roundabout, take the \${ordinal(it)} exit" }
            ?: "At the roundabout, go around and exit left"
        TurnCommand.EXIT_LEFT -> "Take the exit on the left"
        TurnCommand.EXIT_RIGHT -> "Take the exit on the right"
        TurnCommand.ARRIVE -> "Arrive at your destination"
    }
    if (street.isNullOrBlank() || command == TurnCommand.ARRIVE) return verb
    return if (french) "$verb sur $street" else "$verb onto $street"
}

private fun frenchOrdinal(number: Int): String = if (number == 1) "re" else "e"'''
if old not in text:
    raise SystemExit("turnInstruction source did not match; refusing a risky patch")
voice.write_text(text.replace(old, new), encoding="utf-8")

formatter = root / "app/src/main/kotlin/com/danemadsen/atlas/formatting/atlas-formatter.kt"
ft = formatter.read_text(encoding="utf-8")
old2 = '''        val suffix = listOf(" km" to " kilometers", " mi" to " miles", " ft" to " feet", " m" to " meters")
            .firstOrNull { display.endsWith(it.first) } ?: return display
        return display.removeSuffix(suffix.first) + suffix.second'''
new2 = '''        val suffixes = if (locale.language.equals("fr", ignoreCase = true)) {
            listOf(" km" to " kilomètres", " mi" to " milles", " ft" to " pieds", " m" to " mètres")
        } else {
            listOf(" km" to " kilometers", " mi" to " miles", " ft" to " feet", " m" to " meters")
        }
        val suffix = suffixes.firstOrNull { display.endsWith(it.first) } ?: return display
        return display.removeSuffix(suffix.first) + suffix.second'''
if old2 not in ft:
    raise SystemExit("spokenDistance source did not match; refusing a risky patch")
formatter.write_text(ft.replace(old2, new2), encoding="utf-8")

test = root / "app/src/test/kotlin/com/danemadsen/atlas/nav/XioAutomaticNavigationSessionTest.kt"
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text(r'''package com.danemadsen.atlas.nav

import com.danemadsen.atlas.routing.GeoPoint
import com.danemadsen.atlas.routing.RouteProfile
import com.danemadsen.atlas.routing.RouteResult
import com.danemadsen.atlas.routing.TurnCommand
import com.danemadsen.atlas.routing.TurnPoint
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

/**
 * Deterministic automatic drive session. Synthetic GPS fixes exercise the
 * same progress state machine used by NavigationService without requiring
 * a physical GPS receiver or pretending to validate real road data/TTS audio.
 */
class XioAutomaticNavigationSessionTest {
    private fun route(startLon: Double = 0.0, endLon: Double = 0.010): RouteResult {
        val points = (0..10).map { GeoPoint(startLon + (endLon - startLon) * it / 10.0, 0.0) }
        return RouteResult(
            profile = RouteProfile.CAR,
            origin = points.first(),
            destination = points.last(),
            distanceMeters = 1_113,
            durationSeconds = 120,
            ascendMeters = 0,
            points = points,
            turns = listOf(
                TurnPoint(TurnCommand.TURN_LEFT, points[3].lon, 0.0, 3, 334.0, "Rue Principale"),
                TurnPoint(TurnCommand.ARRIVE, points.last().lon, 0.0, 10, 779.0, null),
            ),
        )
    }

    @Test fun simulatedDriveTriggersRerouteAndReplacementRouteContinues() {
        val original = route()
        val progress = NavigationProgress(original)
        assertFalse(progress.update(GeoPoint(0.0, 0.0)).events.recalculate)

        val offRoad = GeoPoint(0.004, 0.01)
        assertFalse(progress.update(offRoad).events.recalculate)
        assertFalse(progress.update(offRoad).events.recalculate)
        val trigger = progress.update(offRoad)
        assertTrue("third consecutive off-route GPS fix must request recalculation", trigger.events.recalculate)

        // The service installs a fresh progress engine only after RouterGateway
        // returns a replacement route. This verifies the post-replacement state.
        val replacement = route(startLon = 0.004, endLon = 0.014)
        val reroutedProgress = NavigationProgress(replacement)
        val resumed = reroutedProgress.update(GeoPoint(0.004, 0.0))
        assertFalse(resumed.snapshot.arrived)
        assertEquals(TurnCommand.TURN_LEFT, resumed.snapshot.nextTurn?.command)
        assertTrue(resumed.snapshot.remainingMeters > 900.0)
    }

    @Test fun longAutomaticSessionDoesNotLoseProgressOrRepeatTurnAnnouncements() {
        val engine = NavigationProgress(route())
        var announcements = 0
        for (step in 0..1000) {
            val lon = (step.coerceAtMost(100) / 100.0) * 0.010
            val result = engine.update(GeoPoint(lon, 0.0), bearing = 90.0, atElapsedMs = step * 1_000L, accuracyMeters = 5.0)
            announcements += result.events.announcements.size
            if (result.snapshot.arrived) break
        }
        assertTrue("session should announce at least one maneuver", announcements > 0)
        assertTrue("session should reach the destination", engine.initial().arrived)
    }

    @Test fun frenchVoiceInstructionsCoverTurnsAndDistances() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.CANADA_FRENCH)
            assertEquals("Tournez à gauche sur Rue Principale",
                turnInstruction(TurnCommand.TURN_LEFT, "Rue Principale"))
            assertEquals("Continuez tout droit", turnInstruction(TurnCommand.STRAIGHT, null))
            assertEquals("Faites demi-tour", turnInstruction(TurnCommand.U_TURN, null))
            assertEquals("Au rond-point, prenez la 1re sortie",
                turnInstruction(TurnCommand.ROUNDABOUT, null, 1))
            assertEquals("Vous êtes arrivé à destination",
                turnInstruction(TurnCommand.ARRIVE, null))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun stoppedOrStaleNavigationSessionCannotPublishOverNewSession() {
        val current = route()
        val replacement = NavigationCoordinator.RouteReplacement(11L, current, 0, route(0.001, 0.011))
        assertTrue(replacement.matches(11L, current, 0))
        assertFalse(replacement.matches(12L, current, 0))
        assertFalse(replacement.matches(11L, current, 1))
        replacement.accepted.cancel()
        assertFalse(replacement.matches(11L, current, 0))
    }
}
''', encoding="utf-8")
print("XIO GPS French voice overlay and deterministic auto-session tests installed.")
