package at.obyoxar.nanoclock.nanoleafconnector

interface Animation{
    fun animate(animator: NanoLeaf.Animator)

    suspend fun onStart(animator: NanoLeaf.Animator) {}
}