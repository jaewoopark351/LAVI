//20260701_kpopmodder: Keeps legacy SAIDA/BWEM sources buildable without editing upstream files.
#pragma once

#include <algorithm>
#include <iostream>
#include <random>

using std::cout;
using std::endl;
using std::max;
using std::min;

template <typename RandomIt>
void random_shuffle(RandomIt first, RandomIt last)
{
    static std::mt19937 generator{std::random_device{}()};
    std::shuffle(first, last, generator);
}
