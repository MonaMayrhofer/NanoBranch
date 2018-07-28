package at.obyoxar.nanobranch.nanoleafconnector

class HttpException(val code: Int, cause: Throwable? = null): Exception("Received Response-code $code", cause)