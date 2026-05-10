import Foundation

enum Formatters {
    static func number(_ value: Double, digits: Int = 1) -> String {
        String(format: "%.\(digits)f", value)
    }

    static func percent(_ value: Double, digits: Int = 1) -> String {
        "\(number(value, digits: digits))%"
    }

    static func bytes(_ value: Int64) -> String {
        ByteCountFormatter.string(fromByteCount: value, countStyle: .binary)
    }

    static func rate(_ value: Int64) -> String {
        "\(bytes(value))/s"
    }

    static func uptime(_ seconds: Int64) -> String {
        let days = seconds / 86_400
        let hours = seconds % 86_400 / 3_600
        let minutes = seconds % 3_600 / 60
        if days > 0 { return "\(days)d \(hours)h" }
        if hours > 0 { return "\(hours)h \(minutes)m" }
        return "\(minutes)m"
    }
}

extension String {
    func trimmedTrailingSlash() -> String {
        var value = self
        while value.hasSuffix("/") {
            value.removeLast()
        }
        return value
    }
}
