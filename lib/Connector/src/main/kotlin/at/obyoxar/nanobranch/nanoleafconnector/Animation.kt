package at.obyoxar.nanobranch.nanoleafconnector

interface Animation{
    fun animate(animator: NanoLeaf.Animator)

    suspend fun onStart(animator: NanoLeaf.Animator) {}
}