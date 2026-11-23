package my.dreamora.core.appPreferences

enum class TernaryState {
    Active,
    Inverse,
    Inactive;

    fun next() = when (this) {
        Active -> Inverse
        Inverse -> Inactive
        Inactive -> Active
    }
}