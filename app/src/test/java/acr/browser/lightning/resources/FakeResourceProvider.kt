package acr.browser.lightning.resources

class FakeResourceProvider : ResourceProvider {
    override fun stringResource(id: Int): String = "test:$id"

    override fun stringResource(id: Int, vararg args: Any): String = error("Not implemented")

    override fun stringArrayResource(id: Int): Array<String> = error("Not implemented")
}
