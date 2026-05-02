package com.altbank.cardsapi.domain.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.Clock;
import java.time.LocalDateTime;

import static com.altbank.cardsapi.domain.model.support.Strings.requireNonBlank;

@Entity
@Table(name = "virtual_cards")
@DiscriminatorValue("VIRTUAL")
@PrimaryKeyJoinColumn(name = "card_id")
@Access(AccessType.FIELD)
public class VirtualCard extends Card {

    @Column(name = "processor_account_id", length = 64, nullable = false)
    private String processorAccountId;

    @Column(name = "processor_card_id", length = 64, nullable = false, unique = true)
    private String processorCardId;

    @Column(name = "cvv_expiration_at")
    private LocalDateTime cvvExpirationAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reissue_reason", length = 24)
    private ReissueReason reissueReason;

    @Column(name = "previous_virtual_card_id", length = 36)
    private String previousVirtualCardId;

    protected VirtualCard() {
    }

    public VirtualCard(Account account,
                       String processorAccountId,
                       String processorCardId,
                       LocalDateTime cvvExpirationAt,
                       Clock clock) {
        this(account, processorAccountId, processorCardId, cvvExpirationAt, null, null, clock);
    }

    public VirtualCard(Account account,
                       String processorAccountId,
                       String processorCardId,
                       LocalDateTime cvvExpirationAt,
                       ReissueReason reissueReason,
                       VirtualCard previousCard,
                       Clock clock) {
        super(account, clock);
        this.processorAccountId = requireNonBlank(processorAccountId, "processorAccountId");
        this.processorCardId = requireNonBlank(processorCardId, "processorCardId");
        this.cvvExpirationAt = cvvExpirationAt;
        this.reissueReason = reissueReason;
        this.previousVirtualCardId = previousCard == null ? null : previousCard.id().toString();
    }

    public String processorAccountId() {
        return processorAccountId;
    }

    public String processorCardId() {
        return processorCardId;
    }

    public LocalDateTime cvvExpirationAt() {
        return cvvExpirationAt;
    }

    public ReissueReason reissueReason() {
        return reissueReason;
    }

    public String previousVirtualCardId() {
        return previousVirtualCardId;
    }

    public void updateCvvExpirationAt(LocalDateTime expirationAt) {
        this.cvvExpirationAt = expirationAt;
    }

    @Override
    public CardType type() {
        return CardType.VIRTUAL;
    }
}
