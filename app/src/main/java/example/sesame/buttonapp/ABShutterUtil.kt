package example.sesame.buttonapp

import android.os.SystemClock
import android.view.KeyEvent
import android.view.KeyEvent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ABShutterUtil {

    private var eventWatching = false
    private val events = ArrayList<KeyEvent>(16)

    fun handleEvent(event: KeyEvent?, callback:(ButtonSequence) -> Unit): Boolean {
        if (event == null) {
            return false
        }
        if (eventWatching) {
            events.add(event)
            return true
        }
        if (isFirstEvent(event)) {
            eventWatching = true
            events.clear()
            events.add(event)
            GlobalScope.launch(Dispatchers.Main) {
                //Timber.d("##### launchEventWatchingLoop")
                while (eventWatching) {
                    if (SystemClock.uptimeMillis() - events[events.lastIndex].eventTime > 350) {
                        // it's been quite long time since the last event
                        // let's check the key sequence 
                        val seq = checkSequence(events)
                        if (seq != ButtonSequence.UNKNOWN) {
                            //Timber.d("##### %s #####", seq.toString())
                            callback(seq)
                        }
                        eventWatching = false
                        break
                    } else {
                        delay(5)
                    }
                } // while
                //Timber.d("##### exitEventWatchingLoop")
            }
            return true
        }
        return false
    }

    private fun isFirstEvent(event: KeyEvent): Boolean {
        var result = false

        event.run {
            if (action == ACTION_DOWN && keyCode == KEYCODE_ENTER && repeatCount == 0) {
                // small button normal start sequence
                result = true
            } else if (action == ACTION_UP && keyCode == KEYCODE_ENTER && repeatCount == 0) {
                // small button abnormal start sequence
                // hack alert: first event might be dropped
                result = true
            } else if (action == ACTION_DOWN && keyCode==KEYCODE_ENTER && repeatCount > 0 && flags == 0x88) {
                // small button long start
                result = true
            } else if (action == ACTION_DOWN && keyCode == KEYCODE_VOLUME_UP && repeatCount == 0) {
                // large button start
                result = true
            } else if (action == ACTION_DOWN && keyCode == KEYCODE_VOLUME_UP && repeatCount > 0 && flags == 0x88) {
                // large button long start
                result = true
            }
        }
        return result
    }

    private fun checkSequence(events: ArrayList<KeyEvent>): ButtonSequence {
        enumValues<ButtonSequence>().forEach {
            if (it.compare(events)) {
                return it
            }
        }
        return ButtonSequence.UNKNOWN
    }

    enum class ButtonSequence {
        UNKNOWN {
            override fun compare(events: ArrayList<KeyEvent>): Boolean {
                return false
            }
            override fun toString(): String {
                return "UNKNOWN"
            }
        },
        SMALL_SHORT {
            override fun toString(): String {
                return "Small SHORT"
            }
            override fun compare(events: ArrayList<KeyEvent>): Boolean {
                var result = false
                if (events.size == 2) {
                    if (events[0].action == ACTION_DOWN && events[0].keyCode == KEYCODE_ENTER && events[0].repeatCount == 0 ) {
                        if ((events[1].action == ACTION_UP && events[1].keyCode == KEYCODE_ENTER)){
                            result = true
                        }
                    }
                } else if (events.size == 1) {
                    // hack alert: the first down event could be dropped
                    if (events[0].action == ACTION_UP && events[0].keyCode == KEYCODE_ENTER && events[0].repeatCount == 0) {
                        result = true
                    }
                }
                return result
            }
        },
        SMALL_DOUBLE {
            override fun toString(): String {
                return "Small DOUBLE"
            }
            override fun compare(events: ArrayList<KeyEvent>): Boolean {
                if (events.size == 4) {
                        return((events[0].action == ACTION_DOWN && events[0].keyCode == KEYCODE_ENTER) &&
                                (events[1].action == ACTION_UP && events[1].keyCode == KEYCODE_ENTER) &&
                                (events[2].action == ACTION_DOWN && events[2].keyCode == KEYCODE_ENTER) &&
                                (events[3].action == ACTION_UP && events[3].keyCode == KEYCODE_ENTER))
                } else if (events.size == 3) {
                    // hack alert: the first down event could be dropped
                    return(
                            (events[0].action == ACTION_UP && events[0].keyCode == KEYCODE_ENTER) &&
                                    (events[1].action == ACTION_DOWN && events[1].keyCode == KEYCODE_ENTER) &&
                                    (events[2].action == ACTION_UP && events[2].keyCode == KEYCODE_ENTER))

                }
                return false
            }
        },
        LARGE_SHORT {
            override fun toString(): String {
                return "Large SHORT"
            }

            override fun compare(events: ArrayList<KeyEvent>): Boolean {
                var result = false
                if (events.size == 2) {
                    if (events[0].action == ACTION_DOWN && events[0].keyCode == KEYCODE_VOLUME_UP && events[0].repeatCount == 0) {
                        if ((events[1].action == ACTION_UP && events[1].keyCode == KEYCODE_VOLUME_UP && events[1].repeatCount == 0)) {
                            result = true
                        }
                    }
                }
                return result
            }
        },
        LARGE_DOUBLE {
            override fun toString(): String {
                return "Large DOUBLE"
            }

            override fun compare(events: ArrayList<KeyEvent>): Boolean {
                return ((events.size == 4) &&
                        (events[0].action == ACTION_DOWN && events[0].keyCode == KEYCODE_VOLUME_UP) &&
                        (events[1].action == ACTION_UP && events[1].keyCode == KEYCODE_VOLUME_UP) &&
                        (events[2].action == ACTION_DOWN && events[2].keyCode == KEYCODE_VOLUME_UP) &&
                        (events[3].action == ACTION_UP && events[3].keyCode == KEYCODE_VOLUME_UP))
            }
        },
        SMALL_LONG {
            override fun toString(): String {
                return "Small LONG"
            }


            override fun compare(events: ArrayList<KeyEvent>): Boolean {
                var result = false
                // first event
                if (events.size > 1) {
                    var ev = events[0]
                    if (ev.action == ACTION_DOWN && ev.keyCode == KEYCODE_ENTER && ev.flags == 0x88) {
                        ev = events[events.lastIndex - 1]
                        if (ev.action == ACTION_DOWN && ev.keyCode == KEYCODE_ENTER && ev.repeatCount > 0) {
                            // last event
                            ev = events[events.lastIndex]
                            if (ev.action == ACTION_UP && ev.keyCode == KEYCODE_ENTER) {
                                result = true
                            }
                        }
                    }
                }
                return result
            }
        },
        LARGE_LONG {
            override fun toString(): String {
                return "Large LONG"
            }

            override fun compare(events: ArrayList<KeyEvent>): Boolean {
                var result = false
                if (events.size > 1) {
                    // first event
                    var ev = events[0]
                    if (ev.action == ACTION_DOWN && ev.keyCode == KEYCODE_VOLUME_UP && ev.flags == 0x88) {
                        ev = events[events.lastIndex -1]
                        if (ev.action == ACTION_DOWN && ev.keyCode == KEYCODE_VOLUME_UP && ev.repeatCount > 0) {
                            // last event
                            ev = events[events.lastIndex]
                            if (ev.action == ACTION_UP && ev.keyCode == KEYCODE_VOLUME_UP) {
                                result = true
                            }
                        }
                    }
                }
                return result
            }
        };

        abstract fun compare(events: ArrayList<KeyEvent>): Boolean

    }
}