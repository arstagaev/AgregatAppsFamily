//
//  DeviceInfo.swift
//  iosApp
//


import UIKit

class DeviceInfo {
    static let shared = DeviceInfo()

    private let userDefaultsKey = "device_id"

    private(set) lazy var deviceId: String = {
        // Prefer system-provided identifierForVendor
        if let vendorId = UIDevice.current.identifierForVendor?.uuidString {
            return vendorId
        }

        // Fallback to stored UUID
        let defaults = UserDefaults.standard
        if let existing = defaults.string(forKey: userDefaultsKey) {
            return existing
        } else {
            let newId = UUID().uuidString
            defaults.set(newId, forKey: userDefaultsKey)
            return newId
        }
    }()

    var deviceSpecificInfo: String {
        let name = UIDevice.current.name.replacingOccurrences(of: " ", with: "")
        let shortId = String(deviceId.suffix(6))
        return "\(name)_\(shortId)"
    }
}
