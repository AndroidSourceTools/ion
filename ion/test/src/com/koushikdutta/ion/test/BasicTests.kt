package com.koushikdutta.ion.test

import android.content.Context
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonObject
import com.koushikdutta.ion.ContextReference
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.IonContext
import com.koushikdutta.ion.builder.IonPromise
import com.koushikdutta.scratch.createScheduler
import com.koushikdutta.scratch.event.sleep
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BasicTests: AsyncTests() {
    val EXIF_ASSET = "file:///android_asset_observe/exif.jpg"
    @Test
    fun testHello() = testAsync {
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load("test://example.com/hello")
                .asString()
                .await()

        assertEquals(result, "hello")
        assertEquals(Thread.currentThread(), Looper.getMainLooper().thread)
    }

    @Test
    fun testHelloNullHandler() = testAsync {
        val testThread = Thread.currentThread()
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load("test://example.com/hello")
                .setHandler(null)
                .asString()
                .await()

        assertEquals(result, "hello")
        assertEquals(Thread.currentThread(), testThread)
    }

    @Test
    fun testEcho() = testAsync {
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load("test://example.com/echo")
                .setStringBody("hello world")
                .asString()
                .await()

        assertEquals(result, "hello world")
    }

    fun validateJson(result: JsonObject) {
        assertEquals(result.get("hello").asString, "world")
        assertEquals(result.get("foo").asString, "bar")
    }

    @Test
    fun testQuery() = testAsync {
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load("test://example.com/query")
                .addQuery("hello", "world")
                .addQuery("foo", "bar")
                .asJsonObject()
                .await()
        validateJson(result)
    }

    @Test
    fun testUrlEncoded() = testAsync {
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load("test://example.com/urlencoded")
                .setBodyParameter("hello", "world")
                .setBodyParameter("foo", "bar")
                .asJsonObject()
                .await()

        validateJson(result)
    }

    @Test
    fun testMultipart() = testAsync {
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load("test://example.com/multipart")
                .setMultipartParameter("hello", "world")
                .setMultipartParameter("foo", "bar")
                .asJsonObject()
                .await()

        validateJson(result)
    }

    @Test
    fun testAsset() = testAsync {
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .asByteArray()
                .await()

        assertEquals(result.size, 33830)
    }


    @Test
    fun testAssetObserver() = testAsync {
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .asByteArray()
                .await()

        assertEquals(result.size, 33830)
        assertEquals(assetObserver.loadObserved, 1)
    }


    @Test
    fun testAssetObserver2() = testAsync {
        // run the test twice to make sure the asset observer gets reset
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .asByteArray()
                .await()

        assertEquals(result.size, 33830)
        assertEquals(assetObserver.loadObserved, 1)
    }

    @Test
    fun testAssetObserver3() = testAsync {
        val result = ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .asByteArray()
                .await()

        assertEquals(result.size, 33830)
        assertEquals(assetObserver.loadObserved, 1)

        ion.build(InstrumentationRegistry.getInstrumentation().context)
                .load(EXIF_ASSET)
                .asByteArray()
                .await()

        assertEquals(assetObserver.loadObserved, 2)
    }

    private val contextReference = ContextReference.fromContext(InstrumentationRegistry.getInstrumentation().context)

    @Test
    fun testIonPromise() = testAsync {
        val affinity = Looper.getMainLooper().createScheduler()
        assertFalse(affinity.isAffinityThread)
        val promise = IonPromise(contextReference, Looper.getMainLooper().createScheduler()) {
            42
        }

        promise.await()
        assertTrue(affinity.isAffinityThread)
    }


    @Test
    fun testIonPromise2() = testAsync {
        val affinity = Looper.getMainLooper().createScheduler()
        assertFalse(affinity.isAffinityThread)
        val promise = IonPromise(contextReference, Looper.getMainLooper().createScheduler()) {
            42
        }

        promise.get()
        assertTrue(!affinity.isAffinityThread)

        promise.await()
        assertTrue(affinity.isAffinityThread)
    }

    class DeadContext : IonContext {
        override fun isAlive() = "dead"
        override fun getContext() = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun testDeadContext() = testAsync(true, true) {
        val result = ion.build(DeadContext())
                .load("test://example.com/hello")
                .asString()
                .await()

        assertEquals(result, "hello")
        assertEquals(Thread.currentThread(), Looper.getMainLooper().thread)
    }
}
