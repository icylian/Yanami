import Foundation

enum AppLocalization {
    static func locale(for language: String) -> Locale {
        guard let identifier = localeIdentifier(for: language) else {
            return .autoupdatingCurrent
        }
        return Locale(identifier: identifier)
    }

    static func string(_ key: String, language: String) -> String {
        guard let identifier = localeIdentifier(for: language),
              let path = Bundle.main.path(forResource: identifier, ofType: "lproj"),
              let bundle = Bundle(path: path) else {
            return NSLocalizedString(key, comment: "")
        }
        return bundle.localizedString(forKey: key, value: key, table: nil)
    }

    private static func localeIdentifier(for language: String) -> String? {
        switch language {
        case "zh": return "zh-Hans"
        case "en": return "en"
        case "ja": return "ja"
        default: return nil
        }
    }
}
