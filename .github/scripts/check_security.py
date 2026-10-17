"""Static repository security invariants; not a substitute for device tests."""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
read = lambda p: (root / p).read_text(encoding="utf-8")
ssh = read("app/src/main/java/com/example/server/SshServerManager.kt")
prefs = read("app/src/main/java/com/example/data/local/ServerPreferences.kt")
model = read("app/src/main/java/com/example/data/model/ServerConfig.kt")
env = read("app/src/main/java/com/example/server/LinuxEnvironment.kt")
manifest = read("app/src/main/AndroidManifest.xml")
checks = {
    "no hardcoded legacy password": "matrix1337" not in (ssh + prefs + model),
    "password auth off by default": "val passwordAuthEnabled: Boolean = false" in model,
    "actual key matching": "KeyUtils.compareKeys(trusted, key)" in ssh,
    "fail-closed startup": "SshAuthPolicy.canStart(config" in ssh,
    "Keystore AES-GCM storage": "AndroidKeyStore" in prefs and "AES/GCM/NoPadding" in prefs,
    "no plaintext password writes": ".putString(KEY_PASSWORD, config.password)" not in prefs,
    "Android backup disabled": 'android:allowBackup="false"' in manifest,
    "fake package manager not generated": "val aptScript = File(binDir," not in env,
    "fake interpreters not generated": "install_binary_wrapper()" not in env,
}
for name, passed in checks.items():
    print(("PASS" if passed else "FAIL") + ": " + name)
if not all(checks.values()):
    raise SystemExit(1)
