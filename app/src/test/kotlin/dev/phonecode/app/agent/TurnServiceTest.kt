package dev.phonecode.app.agent

import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.phonecode.app.PhoneCodeApplication
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TurnServiceTest {
    @Test
    fun notificationStopDoesNotRunStopHandlersOnTheCallbackThread() {
        assertStopIsDispatched {
            it.onStartCommand(
                Intent().setAction("dev.phonecode.app.action.STOP_WORK"),
                0,
                1,
            )
        }
    }

    @Test
    fun foregroundTimeoutDoesNotRunStopHandlersOnTheCallbackThread() {
        assertStopIsDispatched {
            it.onTimeout(1, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        }
    }

    private fun assertStopIsDispatched(stop: (TurnService) -> Unit) {
        val app = ApplicationProvider.getApplicationContext<PhoneCodeApplication>()
        val controller = Robolectric.buildService(TurnService::class.java).create()
        val handlerEntered = CountDownLatch(1)
        val releaseHandler = CountDownLatch(1)
        val callbackReturned = CountDownLatch(1)
        val callbackThread = AtomicReference<Thread>()
        val handlerThread = AtomicReference<Thread>()
        val failure = AtomicReference<Throwable?>()
        app.foregroundLeases.registerStopHandler("turn-service-test") {
            handlerThread.set(Thread.currentThread())
            handlerEntered.countDown()
            releaseHandler.await(5, TimeUnit.SECONDS)
        }
        val caller = thread {
            callbackThread.set(Thread.currentThread())
            runCatching { stop(controller.get()) }.onFailure(failure::set)
            callbackReturned.countDown()
        }

        try {
            assertTrue(handlerEntered.await(5, TimeUnit.SECONDS))
            assertTrue(callbackReturned.await(1, TimeUnit.SECONDS))
            assertNull(failure.get())
            assertNotSame(callbackThread.get(), handlerThread.get())
        } finally {
            releaseHandler.countDown()
            caller.join(5_000)
            app.foregroundLeases.unregisterStopHandler("turn-service-test")
            controller.destroy()
        }
    }
}
