import org.pledgez.Pledges

Pledges.describe("Division By Zero").addBatch {
    "when dividing a number by zero" {
        topic = { 42f / 0f }

        "we get Infinity" { it == Float.POSITIVE_INFINITY }
    }
    "but when dividing zero by zero" {
        topic = { 0f / 0f }

        "we get a value which" {
            "is not a number" { it.isNaN() }
            "is not equal to itseld" { it != it }
        }
    }
}.run()
