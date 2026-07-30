package ai.cyrene.mobile

import android.app.Application
import ai.cyrene.mobile.data.SecureStore
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class CyreneMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        syncApplicationNightMode(this, SecureStore(this).uiTheme())
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
    }
}
