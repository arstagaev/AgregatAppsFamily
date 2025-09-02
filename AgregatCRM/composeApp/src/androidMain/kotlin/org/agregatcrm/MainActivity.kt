package org.agregatcrm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import org.agregatcrm.domain.provideEventsController
import org.agregatcrm.ui.App

class MainActivity : ComponentActivity() {

    private val controller by lazy { provideEventsController(lifecycleScope) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // example: fetch 20 recent for Воронеж
        controller.fullRefresh(
            count = 3,
            ncount = 50,
            filterBy = "ПодразделениеКомпании",
            filterVal = "Воронеж"
        )
        setContent {
            App(scope = lifecycle.coroutineScope, controller)
        }

//        val scope = MainScope()
//        scope.launch {
//            val events = DebugApi.fetchEvents(token = TOKEN)
//            println("Got ${events.size} events")
//            println("Events: ${events.joinToString()}")
//            events.take(1).forEach { ev ->
//                println("First: ${ev.guid} — ${ev.number}")
//            }
//        }
    }
}
