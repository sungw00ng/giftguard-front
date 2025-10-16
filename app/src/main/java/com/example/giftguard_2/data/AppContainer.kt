package com.example.giftguard.data

import com.example.giftguard.viewmodels.GiftconViewModel

/**
 * 앱의 의존성 주입(DI) 컨테이너 역할을 하는 인터페이스 및 구현 클래스입니다.
 * Repository와 ViewModel 인스턴스를 싱글톤으로 제공합니다.
 *
 * @property giftconRepository GiftconRepository 인스턴스
 * @property giftconViewModel GiftconViewModel 인스턴스 (Repository에 의존)
 */
interface AppContainer {
    val giftconRepository: GiftconRepository
    val giftconViewModel: GiftconViewModel
}

/**
 * AppContainer 인터페이스의 실제 구현체입니다.
 * 앱 전체에서 이 싱글톤 인스턴스를 사용합니다.
 */
class DefaultAppContainer : AppContainer {
    // 1. Repository 인스턴스 생성 (데이터 접근 로직)
    override val giftconRepository: GiftconRepository by lazy {
        // TODO: 실제 Firebase 또는 RoomDB 인스턴스를 여기에 전달해야 합니다.
        GiftconRepository()
    }

    // 2. ViewModel 인스턴스 생성 (상태 관리 로직)
    // lazy를 사용하여 앱 전체에서 단 하나의 인스턴스만 생성하도록 보장합니다.
    override val giftconViewModel: GiftconViewModel by lazy {
        // ViewModel을 생성할 때 Repository 의존성을 주입합니다.
        GiftconViewModel(giftconRepository)
    }
}