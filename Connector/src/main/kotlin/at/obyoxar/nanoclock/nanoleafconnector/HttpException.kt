package at.obyoxar.nanoclock.nanoleafconnector

class HttpException(val code: Int, cause: Throwable? = null): Exception("Received Response-code $code", cause)