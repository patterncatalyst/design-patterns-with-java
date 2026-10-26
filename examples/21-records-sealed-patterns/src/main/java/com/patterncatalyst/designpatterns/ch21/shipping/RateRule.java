package com.patterncatalyst.designpatterns.ch21.shipping;

/**
 * A small, closed grammar for how a shipping zone prices a parcel. A rule
 * is built by nesting these four kinds; RateRules walks whatever tree a
 * caller builds, and how the pieces are nested, not just which pieces are
 * present, decides the quoted price.
 */
public sealed interface RateRule permits FlatRate, PerKilogram, ZoneSurcharge, MinimumCharge {
}
