@file:Suppress("NOTHING_TO_INLINE")

package acr.browser.lightning.html.jsoup

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

inline fun parse(string: String): Document = Jsoup.parse(string)

infix fun Document.andBuild(build: Document.() -> Unit): String {
    build()
    return outerHtml()
}

inline fun Document.title(provide: () -> String) {
    this.title(provide())
}

inline fun Document.body(build: Element.() -> Unit) {
    build(body())
}

inline fun Element.tag(tag: String, build: Element.() -> Unit): Element {
    return getElementsByTag(tag).first().also(build)
}

inline fun Element.clone(edit: Element.() -> Unit): Element {
    return clone().also(edit)
}

inline fun Element.id(string: String, build: Element.() -> Unit): Element {
    return getElementById(string).also(build)
}

inline fun Element.id(string: String): Element {
    return getElementById(string)
}

inline fun Element.removeElement(): Element {
    return also(Element::remove)
}

inline fun Element.div(clazz: String, id: String? = null, build: Element.() -> Unit) {
    val element = Element("div").apply {
        attr("class", clazz)
        id?.let { attr("id", id) }
    }
    appendChild(element)
    build(element)
}

inline fun Element.a(href: String) {
    appendChild(Element("a").attr("href", href))
}

inline fun Element.img(src: String) {
    appendChild(Element("img").attr("src", src))
}

inline fun Element.p(clazz: String, id: String? = null, build: Element.() -> Unit) {
    val element = Element("p").apply {
        attr("class", clazz)
        id?.let { attr("id", id) }
    }
    appendChild(element)
    build(element)
}
